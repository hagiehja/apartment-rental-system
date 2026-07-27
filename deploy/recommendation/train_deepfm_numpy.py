#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
DeepFM Trainer (NumPy-only)
============================
DeepFM = FM (bias + linear + 2-order cross) + DNN (high-order nonlinear).

Architecture:
    input x (n, 14)
        |
   +----+----+
   |         |
  FM part   DNN part
   |         |
   +----+----+
        |
   sigmoid -> y_hat

FM part (same as train_fm_numpy.py):
    y_fm = b + sum(w_i x_i) + 0.5 * sum_k [(sum_i V_ik x_i)^2 - sum_i V_ik^2 x_i^2]

DNN part:
    h1 = ReLU(W1 x + c1)     # 14 -> 64
    h2 = ReLU(W2 h1 + c2)    # 64 -> 32
    h3 = ReLU(W3 h2 + c3)    # 32 -> 16
    y_dnn = w_out . h3 + b_out

Final logit = y_fm + y_dnn
Loss = BCE(sigmoid(logit), y)

Supports --label-source synthetic|real (same as FM V2 script).
"""
import os, json, random, time, argparse
import numpy as np
from datetime import datetime

# Reuse building blocks from train_fm_numpy.py
import sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from train_fm_numpy import (
    FEATURE_NAMES, FEATURE_INDEX, N_FEATURES,
    build_features, synthetic_label,
    load_houses, load_user_preferences, load_behavior_pairs,
    gen_predefined_preferences, gen_random_preferences,
    pref_from_db_row,
    build_synthetic_dataset, build_real_dataset,
    compute_auc, infer_preferences_from_behavior,
)


# --------------------------- DeepFM model ---------------------------
class DeepFMModelNumpy:
    """FM + DNN, trained jointly via Adam."""

    def __init__(self, n_features, k=8, dnn_hidden=(64, 32, 16),
                 lr=0.005, l2=1e-5, seed=42):
        rng = np.random.RandomState(seed)
        # FM params
        self.b = np.zeros(1)
        self.w = rng.normal(0, 0.01, size=n_features)
        self.V = rng.normal(0, 0.01, size=(n_features, k))
        # DNN params (He initialization for ReLU layers)
        self.dnn_W = []; self.dnn_b = []
        in_dim = n_features
        for h in dnn_hidden:
            self.dnn_W.append(rng.normal(0, np.sqrt(2.0 / in_dim), size=(in_dim, h)))
            self.dnn_b.append(np.zeros(h))
            in_dim = h
        self.out_w = rng.normal(0, np.sqrt(2.0 / in_dim), size=(in_dim, 1))
        self.out_b = np.zeros(1)
        self.dnn_hidden = dnn_hidden
        self.lr = lr; self.l2 = l2
        # Adam state for every param
        self._params = []
        self._collect_state()

    def _collect_state(self):
        """Initialize Adam m/v for every parameter."""
        self._params = []
        for name, p in [("b", self.b), ("w", self.w), ("V", self.V)]:
            self._params.append((name, p, np.zeros_like(p), np.zeros_like(p)))
        for i, (W, bb) in enumerate(zip(self.dnn_W, self.dnn_b)):
            self._params.append((f"dnn_W{i}", W, np.zeros_like(W), np.zeros_like(W)))
            self._params.append((f"dnn_b{i}", bb, np.zeros_like(bb), np.zeros_like(bb)))
        self._params.append(("out_w", self.out_w, np.zeros_like(self.out_w), np.zeros_like(self.out_w)))
        self._params.append(("out_b", self.out_b, np.zeros_like(self.out_b), np.zeros_like(self.out_b)))

    def _forward_fm(self, X):
        linear = X @ self.w
        XV = X @ self.V; XX = X * X
        XV2 = XX @ (self.V * self.V)
        interaction = 0.5 * np.sum(XV * XV - XV2, axis=1)
        return self.b[0] + linear + interaction, linear, XV, XX

    def _forward_dnn(self, X):
        a = X
        cache = [X]
        for W, b in zip(self.dnn_W, self.dnn_b):
            z = a @ W + b
            a = np.maximum(0, z)  # ReLU
            cache.append((a, z, W))
        logit = (a @ self.out_w + self.out_b).flatten()
        return logit, cache

    def _sigmoid(self, z):
        out = np.empty_like(z)
        out[z >= 35] = 1.0; out[z <= -35] = 0.0
        mask = (z > -35) & (z < 35)
        out[mask] = 1.0 / (1.0 + np.exp(-z[mask]))
        return out

    def predict_proba(self, X):
        X = X.astype(np.float64)
        fm_logit, _, _, _ = self._forward_fm(X)
        dnn_logit, _ = self._forward_dnn(X)
        return self._sigmoid(fm_logit + dnn_logit)

    def _adam_all(self, grads, t):
        """Apply Adam update to all params. grads is dict of name->grad."""
        b1, b2, eps = 0.9, 0.999, 1e-8
        for i, (name, p, m, v) in enumerate(self._params):
            g = grads[name]
            m[:] = b1 * m + (1 - b1) * g
            v[:] = b2 * v + (1 - b2) * (g * g)
            mhat = m / (1 - b1 ** t)
            vhat = v / (1 - b2 ** t)
            p -= self.lr * mhat / (np.sqrt(vhat) + eps)

    def train_batch(self, X, y, X_val=None, y_val=None, epochs=20, batch_size=512, verbose=True):
        X = X.astype(np.float64); y = y.astype(np.float64); n = X.shape[0]
        history = {"loss": [], "val_loss": [], "auc": [], "val_auc": [], "accuracy": [], "val_accuracy": []}
        best_val_auc = -1.0; best_state = None; patience = 4; bad_epochs = 0
        for ep in range(epochs):
            t_ep = time.time()
            perm = np.random.permutation(n); X_ep = X[perm]; y_ep = y[perm]
            total_loss = 0.0; n_batches = 0; t_step = 0
            for i in range(0, n, batch_size):
                t_step += 1
                xb = X_ep[i:i+batch_size]; yb = y_ep[i:i+batch_size]
                nb = xb.shape[0]
                # ---- Forward ----
                fm_logit, linear, XV, XX = self._forward_fm(xb)
                dnn_logit, dnn_cache = self._forward_dnn(xb)
                logit = fm_logit + dnn_logit
                p = self._sigmoid(logit)
                # ---- Loss ----
                dz_logit = (p - yb) / nb   # dL/d(logit), shape (nb,)
                # ---- FM gradients ----
                gb = np.array([dz_logit.sum()])
                gw = xb.T @ dz_logit + self.l2 * self.w
                dz_x = dz_logit[:, None] * xb
                term1 = dz_x.T @ XV
                s = (dz_x * xb).sum(axis=0)
                term2 = self.V * s[:, None]
                gV = term1 - term2 + self.l2 * self.V
                # ---- DNN backward ----
                # Final layer: dL/d(out_w) = h_last.T @ dz_logit[:,None]
                h_last = dnn_cache[-1][0]   # (nb, last_dim)
                g_out_w = h_last.T @ dz_logit[:, None] + self.l2 * self.out_w
                g_out_b = np.array([dz_logit.sum()])
                # Backprop through ReLU layers
                # dnn_cache layout: cache[0]=X, cache[i]=(h_i, z_i, W_i) for i>=1
                # For layer li (0-indexed), input is cache[li] (a_prev),
                #   output/pre-ReLU/weights are in cache[li+1]
                grad_a = (dz_logit[:, None] @ self.out_w.T)  # dL/d(activation_last)
                grads_dnn_W = [None] * len(self.dnn_W)
                grads_dnn_b = [None] * len(self.dnn_b)
                for li in range(len(self.dnn_W) - 1, -1, -1):
                    if li == 0:
                        a_prev = dnn_cache[0]   # X (raw input)
                    else:
                        a_prev = dnn_cache[li][0]   # h_li from previous layer
                    _, z, W = dnn_cache[li + 1]
                    grad_z = grad_a * (z > 0)   # ReLU backward
                    grads_dnn_W[li] = a_prev.T @ grad_z + self.l2 * W
                    grads_dnn_b[li] = grad_z.sum(axis=0)
                    grad_a = grad_z @ W.T
                # ---- Assemble grads dict ----
                grads = {"b": gb, "w": gw, "V": gV,
                         "out_w": g_out_w, "out_b": g_out_b}
                for li in range(len(self.dnn_W)):
                    grads[f"dnn_W{li}"] = grads_dnn_W[li]
                    grads[f"dnn_b{li}"] = grads_dnn_b[li]
                self._adam_all(grads, t_step)
                ll = -np.mean(yb * np.log(p + 1e-12) + (1 - yb) * np.log(1 - p + 1e-12))
                total_loss += ll; n_batches += 1
            avg_loss = total_loss / max(1, n_batches)
            train_auc = compute_auc(y, self.predict_proba(X))
            train_acc = ((self.predict_proba(X) >= 0.5).astype(int) == y.astype(int)).mean()
            history["loss"].append(float(avg_loss))
            history["auc"].append(float(train_auc))
            history["accuracy"].append(float(train_acc))
            if X_val is not None and y_val is not None:
                vp = self.predict_proba(X_val)
                vl = -np.mean(y_val * np.log(vp + 1e-12) + (1 - y_val) * np.log(1 - vp + 1e-12))
                va = compute_auc(y_val, vp)
                vacc = ((vp >= 0.5).astype(int) == y_val.astype(int)).mean()
                history["val_loss"].append(float(vl))
                history["val_auc"].append(float(va))
                history["val_accuracy"].append(float(vacc))
                if verbose:
                    print(f"   ep {ep+1:2d}/{epochs} loss={avg_loss:.4f} auc={train_auc:.4f} acc={train_acc:.4f} "
                          f"| val_loss={vl:.4f} val_auc={va:.4f} val_acc={vacc:.4f} ({time.time()-t_ep:.1f}s)")
                if va > best_val_auc:
                    best_val_auc = va
                    best_state = self._snapshot()
                    bad_epochs = 0
                else:
                    bad_epochs += 1
                    if bad_epochs >= patience:
                        if verbose: print(f"   early stop @ ep{ep+1}, best val_auc={best_val_auc:.4f}")
                        break
            else:
                if verbose:
                    print(f"   ep {ep+1:2d}/{epochs} loss={avg_loss:.4f} auc={train_auc:.4f} acc={train_acc:.4f} ({time.time()-t_ep:.1f}s)")
        if best_state is not None:
            self._restore(best_state)
        return history, best_val_auc

    def _snapshot(self):
        return {
            "b": self.b.copy(), "w": self.w.copy(), "V": self.V.copy(),
            "dnn_W": [W.copy() for W in self.dnn_W],
            "dnn_b": [bb.copy() for bb in self.dnn_b],
            "out_w": self.out_w.copy(), "out_b": self.out_b.copy(),
        }

    def _restore(self, snap):
        self.b = snap["b"].copy(); self.w = snap["w"].copy(); self.V = snap["V"].copy()
        self.dnn_W = [W.copy() for W in snap["dnn_W"]]
        self.dnn_b = [bb.copy() for bb in snap["dnn_b"]]
        self.out_w = snap["out_w"].copy(); self.out_b = snap["out_b"].copy()
        self._collect_state()


# --------------------------- Main ---------------------------
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql-host", default=os.environ.get("MYSQL_HOST", "192.168.24.129"))
    parser.add_argument("--mysql-port", type=int, default=int(os.environ.get("MYSQL_PORT", "3310")))
    parser.add_argument("--mysql-user", default="root")
    parser.add_argument("--mysql-pwd", default="123456")
    parser.add_argument("--mysql-db", default="apartment_db")
    parser.add_argument("--epochs", type=int, default=30)
    parser.add_argument("--batch-size", type=int, default=512)
    parser.add_argument("--k-factors", type=int, default=8)
    parser.add_argument("--dnn-hidden", default="64,32,16",
                        help="comma-separated DNN hidden layer sizes")
    parser.add_argument("--lr", type=float, default=0.005)
    parser.add_argument("--n-samples", type=int, default=100000)
    parser.add_argument("--label-source", choices=["synthetic", "real"], default="real")
    parser.add_argument("--neg-ratio", type=float, default=4.0)
    parser.add_argument("--pos-threshold", type=int, default=8)
    parser.add_argument("--out", default=None)
    parser.add_argument("--metrics-out", default=None)
    args = parser.parse_args()

    dnn_hidden = tuple(int(x) for x in args.dnn_hidden.split(","))
    if args.out is None:
        args.out = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                f"deepfm-model-{args.label_source}.json")
    if args.metrics_out is None:
        args.metrics_out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "metrics-deepfm.json")

    random.seed(42); np.random.seed(42)
    print("=" * 60)
    print(f"  DeepFM Trainer (label-source = {args.label_source}, dnn={dnn_hidden})")
    print("=" * 60)

    print("\n[1/5] loading houses...")
    t0 = time.time()
    try:
        houses = load_houses(args.mysql_host, args.mysql_port, args.mysql_user, args.mysql_pwd, args.mysql_db)
        print(f"   loaded {len(houses)} houses in {time.time()-t0:.1f}s")
    except Exception as e:
        print("   WARN mysql:", e, "-> synthetic houses")
        houses = [{"house_id": i, "city": random.choice(["BJ","SH"]), "district": "d"+str(random.randint(1,5)),
                   "rent_type": random.choice(["WHOLE","SHARED"]), "price": random.randint(2000, 9000),
                   "room_count": random.choice([1, 2, 3]), "view_count": random.randint(0, 500),
                   "status": "AVAILABLE",
                   "create_time": datetime.now() - timedelta(days=random.randint(0, 90))}
                  for i in range(2000)]

    print(f"\n[2/5] building dataset ({args.label_source})...")
    t_ds = time.time()
    if args.label_source == "synthetic":
        X, y = build_synthetic_dataset(houses, args.n_samples)
        dataset_meta = {"source": "synthetic"}
    else:
        print("   loading user_preference...")
        user_prefs = load_user_preferences(args.mysql_host, args.mysql_port, args.mysql_user, args.mysql_pwd, args.mysql_db)
        print(f"   {len(user_prefs):,} preferences")
        print("   loading user_behavior aggregates...")
        pairs = load_behavior_pairs(args.mysql_host, args.mysql_port, args.mysql_user, args.mysql_pwd, args.mysql_db, threshold=args.pos_threshold)
        pos = sum(1 for p in pairs if p[4] == 1)
        print(f"   {len(pairs):,} (user,house) pairs (pos={pos:,}, neg={len(pairs)-pos:,})")
        if len(user_prefs) < 100:
            print(f"   inferring preferences from behavior...")
            house_lookup = {h["house_id"]: h for h in houses}
            user_prefs = infer_preferences_from_behavior(pairs, house_lookup)
            print(f"   inferred preferences for {len(user_prefs):,} users")
        X, y = build_real_dataset(houses, pairs, user_prefs, args.n_samples, args.neg_ratio)
        dataset_meta = {"source": "real", "neg_ratio": args.neg_ratio, "total_pairs": len(pairs),
                        "pos_threshold": args.pos_threshold}
    print(f"   dataset ready in {time.time()-t_ds:.1f}s, pos_rate={y.mean():.4f}")

    print("\n[3/5] train/val split...")
    rng = np.random.RandomState(42)
    perm = rng.permutation(len(X)); val_size = int(len(X) * 0.15)
    val_idx = perm[:val_size]; tr_idx = perm[val_size:]
    X_tr, y_tr = X[tr_idx], y[tr_idx]
    X_val, y_val = X[val_idx], y[val_idx]
    print(f"   train={len(X_tr)}, val={len(X_val)}")

    n_params = 1 + N_FEATURES + N_FEATURES * args.k_factors
    prev = N_FEATURES
    for h in dnn_hidden:
        n_params += prev * h + h
        prev = h
    n_params += dnn_hidden[-1] + 1
    print(f"\n[4/5] training DeepFM (FM k={args.k_factors} + DNN {dnn_hidden}, ~{n_params:,} params)...")
    model = DeepFMModelNumpy(N_FEATURES, k=args.k_factors, dnn_hidden=dnn_hidden,
                              lr=args.lr, l2=1e-5, seed=42)
    t_train = time.time()
    history, best_val_auc = model.train_batch(X_tr, y_tr, X_val, y_val,
        epochs=args.epochs, batch_size=args.batch_size, verbose=True)
    train_elapsed = time.time() - t_train
    final_val_auc = max(history.get("val_auc", [0]))
    final_val_acc = max(history.get("val_accuracy", [0]))
    print(f"   done in {train_elapsed:.1f}s, best val_auc={final_val_auc:.4f}, val_acc={final_val_acc:.4f}")

    print(f"\n[5/5] exporting to {args.out}")
    export = {
        "metadata": {
            "version": f"np-deepfm-{args.label_source}",
            "model": "DeepFM",
            "labelSource": args.label_source,
            "trainedAt": datetime.now().isoformat(),
            "implementation": "numpy-v3",
            "nFeatures": N_FEATURES,
            "kFactors": int(args.k_factors),
            "dnnHidden": list(dnn_hidden),
            "nSamples": int(args.n_samples),
            "negRatio": float(args.neg_ratio) if args.label_source == "real" else None,
            "posRate": float(round(y.mean(), 4)),
            "valAuc": float(round(final_val_auc, 4)),
            "valAccuracy": float(round(final_val_acc, 4)),
            "trainTimeSec": float(round(train_elapsed, 2)),
            "datasetMeta": dataset_meta,
        },
        "bias": round(float(model.b[0]), 6),
        "linearWeights": {FEATURE_NAMES[i]: round(float(model.w[i]), 6) for i in range(N_FEATURES)},
        "factors": {FEATURE_NAMES[i]: [round(float(x), 6) for x in model.V[i]] for i in range(N_FEATURES)},
        "dnnWeights": {
            "layers": [
                {"W": [[round(float(v), 6) for v in row] for row in W],
                 "b": [round(float(v), 6) for v in bb]}
                for W, bb in zip(model.dnn_W, model.dnn_b)
            ],
            "outW": [round(float(v), 6) for v in model.out_w.flatten()],
            "outB": round(float(model.out_b[0]), 6),
        },
    }
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(export, f, ensure_ascii=False, indent=2)
    print(f"   exported. bias={export['bias']:.4f}, out_b={export['dnnWeights']['outB']:.4f}")

    entry = {
        "model": "DeepFM",
        "labelSource": args.label_source,
        "nSamples": int(args.n_samples),
        "kFactors": int(args.k_factors),
        "dnnHidden": list(dnn_hidden),
        "epochs": int(args.epochs),
        "batchSize": int(args.batch_size),
        "negRatio": float(args.neg_ratio) if args.label_source == "real" else None,
        "posRate": float(round(y.mean(), 4)),
        "valAuc": float(round(final_val_auc, 4)),
        "valAccuracy": float(round(final_val_acc, 4)),
        "trainTimeSec": float(round(train_elapsed, 2)),
        "trainedAt": datetime.now().isoformat(),
        "history": history,
    }
    existing = []
    if os.path.exists(args.metrics_out):
        try:
            with open(args.metrics_out, "r", encoding="utf-8") as f:
                existing = json.load(f)
                if not isinstance(existing, list): existing = [existing]
        except Exception: existing = []
    existing.append(entry)
    with open(args.metrics_out, "w", encoding="utf-8") as f:
        json.dump(existing, f, ensure_ascii=False, indent=2)
    print(f"   metrics appended to {args.metrics_out}")

    print("\n[Top5 Feature Importance (FM part)]")
    importance = sorted(zip(FEATURE_NAMES, model.w), key=lambda kv: abs(kv[1]), reverse=True)
    for name, w in importance[:5]:
        print(f"   {name.ljust(24)} w={float(w):.4f}")

    print("\n" + "=" * 60)
    print(f"  DONE! val_auc={final_val_auc:.4f}, val_acc={final_val_acc:.4f}")
    print("=" * 60)


if __name__ == "__main__":
    main()