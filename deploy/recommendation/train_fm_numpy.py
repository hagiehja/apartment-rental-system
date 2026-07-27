#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""NumPy-only FM Trainer V2 - supports synthetic + real-label modes."""
import os, json, random, time, argparse
import numpy as np
from datetime import datetime, timedelta

FEATURE_NAMES = [
    "match.city", "match.district", "match.rentType",
    "price.inRange", "price.nearRange", "price.bucket.mid",
    "room.exact", "room.near",
    "status.available", "status.rented",
    "popularity.high", "popularity.medium",
    "freshness.week", "freshness.month",
]
FEATURE_INDEX = {n: i for i, n in enumerate(FEATURE_NAMES)}
N_FEATURES = len(FEATURE_NAMES)


def build_features(pref, house):
    feats = np.zeros(N_FEATURES, dtype=np.float32)
    if pref.get("city") and house.get("city") and pref["city"].lower() == house["city"].lower():
        feats[FEATURE_INDEX["match.city"]] = 1.0
    if pref.get("district") and house.get("district") and pref["district"].lower() == house["district"].lower():
        feats[FEATURE_INDEX["match.district"]] = 1.0
    if pref.get("rentType") and house.get("rent_type") and pref["rentType"].lower() == house["rent_type"].lower():
        feats[FEATURE_INDEX["match.rentType"]] = 1.0
    price = house.get("price") or 0
    if 3000 <= price <= 7000:
        feats[FEATURE_INDEX["price.bucket.mid"]] = 1.0
    if pref.get("min_price") is not None and pref.get("max_price") is not None:
        if pref["min_price"] <= price <= pref["max_price"]:
            feats[FEATURE_INDEX["price.inRange"]] = 1.0
        elif pref.get("max_price") is not None and price <= pref["max_price"] * 1.15:
            feats[FEATURE_INDEX["price.nearRange"]] = 1.0
    if pref.get("room_count") is not None and house.get("room_count") is not None:
        d = abs(pref["room_count"] - house["room_count"])
        if d == 0:
            feats[FEATURE_INDEX["room.exact"]] = 1.0
        elif d == 1:
            feats[FEATURE_INDEX["room.near"]] = 1.0
    if house.get("status") == "AVAILABLE":
        feats[FEATURE_INDEX["status.available"]] = 1.0
    elif house.get("status") == "RENTED":
        feats[FEATURE_INDEX["status.rented"]] = 1.0
    views = house.get("view_count") or 0
    if views >= 300:
        feats[FEATURE_INDEX["popularity.high"]] = 1.0
    elif views >= 50:
        feats[FEATURE_INDEX["popularity.medium"]] = 1.0
    ct = house.get("create_time")
    if ct is not None:
        try:
            if isinstance(ct, str):
                ct = datetime.fromisoformat(ct.replace("Z", ""))
            days = max(0, (datetime.now() - ct).days)
            if days <= 7:
                feats[FEATURE_INDEX["freshness.week"]] = 1.0
            elif days <= 30:
                feats[FEATURE_INDEX["freshness.month"]] = 1.0
        except Exception:
            pass
    return feats


def synthetic_label(pref, house):
    score = 0.0
    if pref.get("city") and house.get("city") and pref["city"].lower() == house["city"].lower():
        score += 0.25
    if pref.get("district") and house.get("district") and pref["district"].lower() == house["district"].lower():
        score += 0.30
    if pref.get("rentType") and house.get("rent_type") and pref["rentType"].lower() == house["rent_type"].lower():
        score += 0.15
    price = house.get("price") or 0
    if pref.get("min_price") and pref.get("max_price") and pref["min_price"] <= price <= pref["max_price"]:
        score += 0.30
    if house.get("status") == "AVAILABLE":
        score += 0.15
    elif house.get("status") == "RENTED":
        score -= 0.10
    if house.get("view_count", 0) >= 300:
        score += 0.10
    score += random.uniform(-0.08, 0.08)
    return 1 if score >= 0.45 else 0


def load_houses(host, port, user, pwd, db):
    import pymysql
    conn = pymysql.connect(host=host, port=port, user=user, password=pwd, database=db, charset="utf8mb4")
    sql = ("SELECT house_id, city, district, rent_type, price, room_count, view_count, status, create_time "
           "FROM house WHERE status IN ('AVAILABLE','RENTED') LIMIT 20000")
    rows = []
    try:
        cur = conn.cursor(); cur.execute(sql)
        for r in cur.fetchall():
            rows.append({"house_id": r[0], "city": r[1], "district": r[2], "rent_type": r[3],
                         "price": float(r[4]) if r[4] else 0, "room_count": r[5],
                         "view_count": r[6] or 0, "status": r[7], "create_time": r[8]})
    finally:
        conn.close()
    return rows


def load_user_preferences(host, port, user, pwd, db):
    import pymysql
    try:
        conn = pymysql.connect(host=host, port=port, user=user, password=pwd, database=db, charset="utf8mb4")
        sql = ("SELECT user_id, city, district, min_price, max_price, room_count, rent_type FROM user_preference")
        cur = conn.cursor(); cur.execute(sql)
        out = {}
        for r in cur.fetchall():
            out[r[0]] = {"city": r[1], "district": r[2],
                         "min_price": float(r[3]) if r[3] else None,
                         "max_price": float(r[4]) if r[4] else None,
                         "room_count": r[5], "rent_type": r[6]}
        conn.close(); return out
    except Exception as e:
        print("   WARN pref load:", e); return {}


def load_behavior_pairs(host, port, user, pwd, db, threshold=4):
    import pymysql
    conn = pymysql.connect(host=host, port=port, user=user, password=pwd, database=db, charset="utf8mb4")
    sql = ("SELECT user_id, house_id, "
           "SUM(CASE behavior_type WHEN 'PAY' THEN 10 WHEN 'ORDER' THEN 8 WHEN 'FAVORITE' THEN 4 "
           "WHEN 'CLICK' THEN 2 ELSE 1 END) AS ts, "
           "MAX(CASE behavior_type WHEN 'PAY' THEN 1 WHEN 'ORDER' THEN 1 WHEN 'FAVORITE' THEN 1 ELSE 0 END) AS hd "
           "FROM user_behavior GROUP BY user_id, house_id")
    cur = conn.cursor(); cur.execute(sql)
    rows = cur.fetchall(); conn.close()
    out = []
    for r in rows:
        uid, hid, ts, hd = r
        label = 1 if (ts >= threshold or hd == 1) else 0
        out.append((uid, hid, int(ts), int(hd), label))
    return out


def infer_preferences_from_behavior(pairs, house_lookup):
    """
    Infer per-user preference from their positive behaviors.
    Used when user_preference table is empty.
    Strategy:
      - For each user, collect their positively-labeled houses
      - city = weighted-most-common city among their positive houses
      - price: 25-75 percentile of their positive houses' prices
      - rent_type / room_count: most common
    Fallback if user has no positives: use ALL their behavior houses.
    """
    from collections import defaultdict, Counter
    user_pos_houses = defaultdict(list)
    user_all_houses = defaultdict(list)
    for uid, hid, ts, hd, lbl in pairs:
        user_all_houses[uid].append((hid, ts))
        if lbl == 1:
            user_pos_houses[uid].append((hid, ts))

    prefs = {}
    for uid, hlist in user_all_houses.items():
        pos_list = user_pos_houses.get(uid, hlist)
        cities = Counter(); rents = Counter(); rooms = Counter(); prices = []
        for hid, sc in pos_list:
            h = house_lookup.get(hid)
            if not h: continue
            w = max(1, sc)
            if h.get("city"): cities[h["city"]] += w
            if h.get("rent_type"): rents[h["rent_type"]] += w
            if h.get("room_count") is not None: rooms[int(h["room_count"])] += w
            if h.get("price"): prices.append(float(h["price"]))
        if not cities:
            continue
        top_city = cities.most_common(1)[0][0]
        top_rent = rents.most_common(1)[0][0] if rents else None
        top_room = rooms.most_common(1)[0][0] if rooms else None
        if prices:
            pn = np.array(prices)
            min_p = float(np.percentile(pn, 25)); max_p = float(np.percentile(pn, 75))
            if max_p - min_p < 500:
                mid = (min_p + max_p) / 2; min_p = max(500, mid - 500); max_p = mid + 500
        else:
            min_p = None; max_p = None
        prefs[uid] = {"city": top_city, "district": None, "rentType": top_rent,
                      "min_price": min_p, "max_price": max_p, "room_count": top_room}
    return prefs


def gen_predefined_preferences():
    presets = []
    for city in ["北京市", "上海市", "深圳市", "广州市", "杭州市", "重庆市", None]:
        for rent in ["WHOLE", "SHARED", None]:
            for rooms in [1, 2, 3, None]:
                for pr in [(2000, 5000), (3000, 7000), (5000, 10000), (None, None)]:
                    presets.append({"city": city, "district": None, "rentType": rent,
                                    "min_price": pr[0], "max_price": pr[1], "room_count": rooms})
    return presets


def gen_random_preferences(n, cities):
    return [{
        "city": random.choice(cities) if cities else None,
        "district": None,
        "rentType": random.choice(["WHOLE", "SHARED", None]),
        "min_price": random.choice([1000, 2000, 3000, None]),
        "max_price": random.choice([5000, 7000, 10000, None]),
        "room_count": random.choice([1, 2, 3, None]),
    } for _ in range(n)]


def pref_from_db_row(row):
    return {"city": row.get("city"), "district": row.get("district"),
            "rentType": row.get("rent_type"),
            "min_price": row.get("min_price"), "max_price": row.get("max_price"),
            "room_count": row.get("room_count")}


def build_synthetic_dataset(houses, n_samples, seed=42):
    random.seed(seed)
    prefs = gen_predefined_preferences()
    cities = list({h["city"] for h in houses if h.get("city")})[:10]
    prefs += gen_random_preferences(max(50, n_samples // 2000), cities)
    X = np.zeros((n_samples, N_FEATURES), dtype=np.float32)
    y = np.zeros((n_samples,), dtype=np.float32)
    for i in range(n_samples):
        pref = random.choice(prefs); h = random.choice(houses)
        X[i] = build_features(pref, h); y[i] = synthetic_label(pref, h)
    return X, y


def build_real_dataset(houses, pairs, user_prefs, n_samples, neg_ratio=3.0, seed=42):
    """
    Real-label dataset builder.

    Strategy: prefer REAL pairs only (positives + real-negatives).
    If real negatives are insufficient to meet neg_ratio, top up with random pairs
    using houses (no user preference), but those will fall back to house-attribute features.

    To keep quality high, we cap n_pos at len(pos) so we never oversample positives.
    """
    random.seed(seed); np.random.seed(seed)
    house_lookup = {h["house_id"]: h for h in houses}
    pos = [(p[0], p[1]) for p in pairs if p[4] == 1]
    real_neg = [(p[0], p[1]) for p in pairs if p[4] == 0]
    print(f"   positives: {len(pos):,} | real negatives (VIEW-only): {len(real_neg):,}")

    # Cap target counts by availability
    n_pos = min(len(pos), max(1000, int(n_samples / (1 + neg_ratio))))
    n_neg_target = min(len(real_neg), n_pos * int(neg_ratio))
    # If real negatives insufficient, allow oversample (with replacement)
    use_rand_neg = 0
    if n_neg_target < n_pos * int(neg_ratio):
        # Prefer oversampling real negatives rather than using random houses
        n_neg_target = n_pos * int(neg_ratio)
    print(f"   target: pos={n_pos:,}, neg={n_neg_target:,} (ratio 1:{n_pos and n_neg_target//n_pos})")

    def sample(src, n):
        if len(src) >= n:
            return random.sample(src, n)
        # with replacement when not enough
        return [random.choice(src) for _ in range(n)]

    pos_s = sample(pos, n_pos)
    rneg_s = sample(real_neg, n_neg_target) if real_neg else []
    all_pairs = pos_s + rneg_s
    all_labels = [1] * len(pos_s) + [0] * len(rneg_s)
    combined = list(zip(all_pairs, all_labels)); random.shuffle(combined)
    X = np.zeros((len(combined), N_FEATURES), dtype=np.float32)
    y = np.zeros((len(combined),), dtype=np.float32)
    miss = 0
    for i, ((uid, hid), lbl) in enumerate(combined):
        h = house_lookup.get(hid) or random.choice(houses)
        row = user_prefs.get(uid)
        if row: pref = pref_from_db_row(row)
        else:
            pref = {"city": h.get("city"), "district": h.get("district"), "rentType": h.get("rent_type"),
                    "min_price": None, "max_price": None, "room_count": None}
            miss += 1
        X[i] = build_features(pref, h); y[i] = lbl
    print(f"   sampled {len(combined):,} pairs (pos={int(y.sum()):,}, neg={len(y)-int(y.sum()):,})")
    print(f"   users without preference row (fallback): {miss:,}")
    return X, y


def compute_auc(y_true, y_score):
    y_true = np.asarray(y_true).astype(int); y_score = np.asarray(y_score)
    n_pos = int(y_true.sum()); n_neg = len(y_true) - n_pos
    if n_pos == 0 or n_neg == 0: return 0.5
    order = np.argsort(y_score); ranks = np.empty(len(y_score), dtype=np.float64)
    sorted_scores = y_score[order]; i = 0
    while i < len(sorted_scores):
        j = i
        while j + 1 < len(sorted_scores) and sorted_scores[j+1] == sorted_scores[i]: j += 1
        ranks[order[i:j+1]] = (i + j) / 2 + 1
        i = j + 1
    sum_ranks_pos = ranks[y_true == 1].sum()
    return float((sum_ranks_pos - n_pos * (n_pos + 1) / 2) / (n_pos * n_neg))


class FMModelNumpy:
    def __init__(self, n_features, k=8, lr=0.01, l2=1e-5, seed=42):
        rng = np.random.RandomState(seed)
        self.b = np.zeros(1); self.w = rng.normal(0, 0.01, size=n_features)
        self.V = rng.normal(0, 0.01, size=(n_features, k))
        self.lr = lr; self.l2 = l2
        self._mb = np.zeros_like(self.b); self._vb = np.zeros_like(self.b)
        self._mw = np.zeros_like(self.w); self._vw = np.zeros_like(self.w)
        self._mV = np.zeros_like(self.V); self._vV = np.zeros_like(self.V)
        self._t = 0; self.b1 = 0.9; self.b2 = 0.999; self.eps = 1e-8

    def _forward(self, X):
        linear = X @ self.w
        XV = X @ self.V; XX = X * X
        XV2 = XX @ (self.V * self.V)
        interaction = 0.5 * np.sum(XV * XV - XV2, axis=1)
        return self.b[0] + linear + interaction, linear, XV, XX

    def _sigmoid(self, z):
        out = np.empty_like(z)
        out[z >= 35] = 1.0; out[z <= -35] = 0.0
        mask = (z > -35) & (z < 35)
        out[mask] = 1.0 / (1.0 + np.exp(-z[mask]))
        return out

    def predict_proba(self, X):
        X = X.astype(np.float64)
        logit, _, _, _ = self._forward(X)
        return self._sigmoid(logit)

    def _adam(self, param, m, v, grad):
        m[:] = self.b1 * m + (1 - self.b1) * grad
        v[:] = self.b2 * v + (1 - self.b2) * (grad * grad)
        mhat = m / (1 - self.b1 ** self._t); vhat = v / (1 - self.b2 ** self._t)
        param -= self.lr * mhat / (np.sqrt(vhat) + self.eps)

    def train_batch(self, X, y, X_val=None, y_val=None, epochs=20, batch_size=512, verbose=True):
        X = X.astype(np.float64); y = y.astype(np.float64); n = X.shape[0]
        history = {"loss": [], "val_loss": [], "auc": [], "val_auc": [], "accuracy": [], "val_accuracy": []}
        best_val_auc = -1.0; best_state = None; patience = 4; bad_epochs = 0
        for ep in range(epochs):
            self._t = 0
            perm = np.random.permutation(n); X_ep = X[perm]; y_ep = y[perm]
            t_ep = time.time(); total_loss = 0.0; n_batches = 0
            for i in range(0, n, batch_size):
                self._t += 1
                xb = X_ep[i:i+batch_size]; yb = y_ep[i:i+batch_size]
                logit, linear, XV, XX = self._forward(xb)
                p = self._sigmoid(logit); nb = xb.shape[0]
                dz = (p - yb) / nb
                gb = np.array([dz.sum()])
                gw = xb.T @ dz + self.l2 * self.w
                # Correct FM gradient for V (shape (d, k)):
                #   dL/dV_jk = sum_n dz_n * x_nj * z_nk - sum_n dz_n * x_nj^2 * V_jk
                #   = term1[j,k] - V_jk * s[j]
                # where:
                #   term1 = (dz[:, None] * x).T @ z   # shape (d, k)
                #   s[j]  = sum_n (dz_n * x_nj^2)     # shape (d,)
                dz_x = dz[:, None] * xb                       # (n, d)
                term1 = dz_x.T @ XV                           # (d, k)
                s = (dz_x * xb).sum(axis=0)                   # (d,)
                term2 = self.V * s[:, None]                   # (d, k)
                gV = term1 - term2 + self.l2 * self.V
                self._adam(self.b, self._mb, self._vb, gb)
                self._adam(self.w, self._mw, self._vw, gw)
                self._adam(self.V, self._mV, self._vV, gV)
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
                    print(f"   ep {ep+1:2d}/{epochs} loss={avg_loss:.4f} auc={train_auc:.4f} acc={train_acc:.4f} | val_loss={vl:.4f} val_auc={va:.4f} val_acc={vacc:.4f} ({time.time()-t_ep:.1f}s)")
                if va > best_val_auc:
                    best_val_auc = va; best_state = (self.b.copy(), self.w.copy(), self.V.copy()); bad_epochs = 0
                else:
                    bad_epochs += 1
                    if bad_epochs >= patience:
                        if verbose: print(f"   early stop @ ep{ep+1}, best val_auc={best_val_auc:.4f}")
                        break
            else:
                if verbose:
                    print(f"   ep {ep+1:2d}/{epochs} loss={avg_loss:.4f} auc={train_auc:.4f} acc={train_acc:.4f} ({time.time()-t_ep:.1f}s)")
        if best_state is not None:
            self.b, self.w, self.V = best_state
        return history, best_val_auc


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql-host", default=os.environ.get("MYSQL_HOST", "192.168.24.129"))
    parser.add_argument("--mysql-port", type=int, default=int(os.environ.get("MYSQL_PORT", "3310")))
    parser.add_argument("--mysql-user", default="root")
    parser.add_argument("--mysql-pwd", default="123456")
    parser.add_argument("--mysql-db", default="apartment_db")
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--batch-size", type=int, default=512)
    parser.add_argument("--k-factors", type=int, default=8)
    parser.add_argument("--lr", type=float, default=0.01)
    parser.add_argument("--n-samples", type=int, default=80000)
    parser.add_argument("--label-source", choices=["synthetic", "real"], default="synthetic")
    parser.add_argument("--neg-ratio", type=float, default=3.0)
    parser.add_argument("--pos-threshold", type=int, default=4,
                        help="(real mode) minimum behavior score to be positive. "
                             "4=FAVORITE+, 8=ORDER+, 10=PAY only")
    parser.add_argument("--out", default=None)
    parser.add_argument("--metrics-out", default=None)
    args = parser.parse_args()

    if args.out is None:
        args.out = os.path.join(os.path.dirname(os.path.abspath(__file__)), f"fm-model-{args.label_source}.json")
    if args.metrics_out is None:
        args.metrics_out = os.path.join(os.path.dirname(os.path.abspath(__file__)), "metrics.json")

    random.seed(42); np.random.seed(42)
    print("=" * 60)
    print(f"  NumPy FM Trainer (label-source = {args.label_source})")
    print("=" * 60)

    print("\n[1/5] loading houses...")
    t0 = time.time()
    try:
        houses = load_houses(args.mysql_host, args.mysql_port, args.mysql_user, args.mysql_pwd, args.mysql_db)
        print(f"   loaded {len(houses)} houses in {time.time()-t0:.1f}s")
    except Exception as e:
        print("   WARN mysql:", e, "-> synthetic houses")
        houses = [{"house_id": i, "city": random.choice(["北京市","上海市"]), "district": "d"+str(random.randint(1,5)),
                   "rent_type": random.choice(["WHOLE","SHARED"]), "price": random.randint(2000, 9000),
                   "room_count": random.choice([1, 2, 3]), "view_count": random.randint(0, 500),
                   "status": "AVAILABLE", "create_time": datetime.now() - timedelta(days=random.randint(0, 90))}
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
        # If user_preference table is empty, infer preferences from behavior
        if len(user_prefs) < 100:
            print(f"   user_preference table too small ({len(user_prefs)} rows), "
                  f"inferring preferences from behavior...")
            house_lookup = {h["house_id"]: h for h in houses}
            user_prefs = infer_preferences_from_behavior(pairs, house_lookup)
            print(f"   inferred preferences for {len(user_prefs):,} users from their behaviors")
        X, y = build_real_dataset(houses, pairs, user_prefs, args.n_samples, args.neg_ratio)
        dataset_meta = {"source": "real", "neg_ratio": args.neg_ratio, "total_pairs": len(pairs),
                        "prefs_inferred": len(user_prefs) < 100}
    print(f"   dataset ready in {time.time()-t_ds:.1f}s, pos_rate={y.mean():.4f}")

    print("\n[3/5] train/val split...")
    rng = np.random.RandomState(42)
    perm = rng.permutation(len(X)); val_size = int(len(X) * 0.15)
    val_idx = perm[:val_size]; tr_idx = perm[val_size:]
    X_tr, y_tr = X[tr_idx], y[tr_idx]
    X_val, y_val = X[val_idx], y[val_idx]
    print(f"   train={len(X_tr)}, val={len(X_val)}")

    print(f"\n[4/5] training FM (k={args.k_factors}, lr={args.lr}, epochs={args.epochs})...")
    model = FMModelNumpy(N_FEATURES, k=args.k_factors, lr=args.lr, l2=1e-5, seed=42)
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
            "version": f"np-fm-{args.label_source}",
            "labelSource": args.label_source,
            "trainedAt": datetime.now().isoformat(),
            "implementation": "numpy-v2",
            "nFeatures": N_FEATURES,
            "kFactors": int(args.k_factors),
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
    }
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(export, f, ensure_ascii=False, indent=2)
    print(f"   exported. bias={export['bias']:.4f}")

    entry = {
        "labelSource": args.label_source, "nSamples": int(args.n_samples),
        "kFactors": int(args.k_factors), "epochs": int(args.epochs),
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

    print("\n[Top5 Feature Importance]")
    importance = sorted(zip(FEATURE_NAMES, model.w), key=lambda kv: abs(kv[1]), reverse=True)
    for name, w in importance[:5]:
        print(f"   {name.ljust(24)} w={float(w):.4f}")

    print("\n" + "=" * 60)
    print(f"  DONE! val_auc={final_val_auc:.4f}, val_acc={final_val_acc:.4f}")
    print("=" * 60)


if __name__ == "__main__":
    main()