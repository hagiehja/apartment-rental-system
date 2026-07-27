#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
TensorFlow 2.x FM (Factorization Machines) 训练脚本 (V2 增强版)
====================================================
功能:
  1. 从 MySQL 读 apartment_db.house 数据 (+ user_behavior 真实行为)
  2. 与 Java 端 FmFeatureBuilder 完全对齐,构建 14 维特征
  3. 标签来源两种模式:
       --label-source synthetic (V1, 规则合成)
       --label-source real      (V2, 真实行为日志, 漏斗加权)
  4. TensorFlow 2.x 自定义 FM 层训练(bias + linear + 二阶交叉)
  5. 导出 Java 兼容的 fm-model-v{N}.json
  6. 输出训练指标供 AUC 对比实验
"""
import os
import json
import random
import argparse
from datetime import datetime, timedelta

import numpy as np
import pandas as pd

FEATURE_NAMES = [
    "match.city", "match.district", "match.rentType",
    "price.inRange", "price.nearRange", "price.bucket.mid",
    "room.exact", "room.near",
    "status.available", "status.rented",
    "popularity.high", "popularity.medium",
    "freshness.week", "freshness.month",
]
FEATURE_INDEX = {name: i for i, name in enumerate(FEATURE_NAMES)}
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


# ----------------------------------------------------------------------------
# V2: Real behavior labels (reads user_behavior table)
# ----------------------------------------------------------------------------
# 漏斗加权: PAY(10) > ORDER(8) > FAVORITE(4) > CLICK(2) > VIEW(1)
# 正样本定义: user×house 的累计行为分数 >= 4 (即至少一次 FAVORITE/ORDER/PAY 或多次 CLICK)
# 负样本定义: 仅有 VIEW 行为 (无任何深度交互)
REAL_LABEL_THRESHOLD = 4

BEHAVIOR_WEIGHT = {
    "VIEW": 1, "CLICK": 2, "FAVORITE": 4, "ORDER": 8, "PAY": 10,
}


def load_user_preferences_from_db(host, port, user, pwd, db):
    """从 user_preference 表加载真实偏好 (用于特征构建)"""
    import pymysql
    try:
        conn = pymysql.connect(host=host, port=port, user=user, password=pwd,
                               database=db, charset="utf8mb4")
        sql = ("SELECT user_id, city, district, min_price, max_price, room_count, rent_type "
               "FROM user_preference")
        df = pd.read_sql(sql, conn)
        conn.close()
        return {row["user_id"]: row for _, row in df.iterrows()}
    except Exception as e:
        print("   WARN user_preference load failed:", e)
        return {}


def load_behavior_labels_from_db(host, port, user, pwd, db, min_score=None):
    """
    从 user_behavior 表构建 (user_id, house_id) -> label 映射
    label = 1 if sum(behavior_score) >= threshold else 0
    同时返回 (user_id, house_id, score) 列表用于采样
    """
    import pymysql
    if min_score is None:
        min_score = REAL_LABEL_THRESHOLD
    conn = pymysql.connect(host=host, port=port, user=user, password=pwd,
                           database=db, charset="utf8mb4")
    sql = """
        SELECT user_id, house_id,
               SUM(CASE behavior_type
                   WHEN 'PAY' THEN 10
                   WHEN 'ORDER' THEN 8
                   WHEN 'FAVORITE' THEN 4
                   WHEN 'CLICK' THEN 2
                   ELSE 1 END) AS total_score,
               COUNT(*) AS event_count,
               MAX(CASE behavior_type WHEN 'PAY' THEN 1
                                      WHEN 'ORDER' THEN 1
                                      WHEN 'FAVORITE' THEN 1 ELSE 0 END) AS has_deep
        FROM user_behavior
        GROUP BY user_id, house_id
    """
    df = pd.read_sql(sql, conn)
    conn.close()
    # 正样本: 累计深度行为 (>=4) 或者有 ORDER/PAY
    df["label"] = ((df["total_score"] >= min_score) | (df["has_deep"] == 1)).astype(int)
    return df


def build_fm_model(n_features, k=8):
    import tensorflow as tf
    from tensorflow.keras import layers, Model

    class FMLayer(layers.Layer):
        def __init__(self, n_features, k, **kwargs):
            super().__init__(**kwargs)
            self.n_features = n_features
            self.k = k

        def build(self, input_shape):
            self.bias = self.add_weight(name="bias", shape=(1,),
                                        initializer="zeros", trainable=True)
            self.linear = self.add_weight(name="linear", shape=(self.n_features,),
                                          initializer="glorot_uniform", trainable=True)
            self.factors = self.add_weight(name="factors", shape=(self.n_features, self.k),
                                           initializer="glorot_uniform", trainable=True)
            super().build(input_shape)

        def call(self, x):
            linear_term = tf.reduce_sum(x * self.linear, axis=1, keepdims=True)
            vx = tf.matmul(x, self.factors)
            sum_then_sq = tf.square(tf.reduce_sum(vx, axis=1, keepdims=True))
            sq_then_sum = tf.reduce_sum(tf.square(vx), axis=1, keepdims=True)
            cross_term = 0.5 * (sum_then_sq - sq_then_sum)
            return self.bias + linear_term + cross_term

    inputs = layers.Input(shape=(n_features,), name="features", dtype=tf.float32)
    logit = FMLayer(n_features, k, name="fm")(inputs)
    outputs = layers.Activation("sigmoid", name="prob")(logit)
    return Model(inputs, outputs, name="FM_Recommender")


def load_houses_from_mysql(host, port, user, pwd, db, limit=20000):
    import pymysql
    conn = pymysql.connect(host=host, port=port, user=user, password=pwd,
                           database=db, charset="utf8mb4")
    sql = ("SELECT house_id, city, district, rent_type, price, room_count, "
           "view_count, status, create_time FROM house "
           "WHERE status IN ('AVAILABLE','RENTED') ORDER BY house_id LIMIT %s")
    df = pd.read_sql(sql, conn, params=(limit,))
    conn.close()
    return df


def gen_random_preferences(n_users, cities):
    prefs = []
    for _ in range(n_users):
        prefs.append({
            "city": random.choice(cities) if cities else None,
            "district": None,
            "rentType": random.choice(["WHOLE", "SHARED", None]),
            "min_price": random.choice([1000, 2000, 3000, None]),
            "max_price": random.choice([5000, 7000, 10000, None]),
            "room_count": random.choice([1, 2, 3, None]),
        })
    return prefs


def gen_predefined_preferences():
    presets = []
    for city in ["北京市", "上海市", "深圳市", "广州市", "杭州市", "重庆市", None]:
        for rent in ["WHOLE", "SHARED", None]:
            for rooms in [1, 2, 3, None]:
                for price_range in [(2000, 5000), (3000, 7000), (5000, 10000), (None, None)]:
                    presets.append({
                        "city": city, "district": None, "rentType": rent,
                        "min_price": price_range[0], "max_price": price_range[1],
                        "room_count": rooms,
                    })
    return presets


def pref_from_db_row(row):
    """Convert user_preference DB row to preference dict used by build_features"""
    return {
        "city": row.get("city"),
        "district": row.get("district"),
        "rentType": row.get("rent_type"),
        "min_price": float(row["min_price"]) if row.get("min_price") is not None else None,
        "max_price": float(row["max_price"]) if row.get("max_price") is not None else None,
        "room_count": int(row["room_count"]) if row.get("room_count") is not None else None,
    }


def build_real_label_dataset(houses_df, behavior_df, user_prefs_db,
                              n_samples, neg_ratio=3.0, random_seed=42):
    """
    构建基于真实行为标签的训练数据集

    正样本来源: behavior_df 中 label=1 的 (user, house) 对
    负样本来源:
      - 真实负样本: behavior_df 中 label=0 的 (user, house) 对 (仅有 VIEW)
      - 随机负样本: 从 houses 池中随机采样 (扩充负样本规模)

    返回: X (n_samples, 14), y (n_samples,), 同时返回真实用户偏好统计
    """
    random.seed(random_seed)
    np.random.seed(random_seed)

    # 把 house_df 转成 dict by id
    house_lookup = {row["house_id"]: row for _, row in houses_df.iterrows()}
    houses_records = houses_df.to_dict("records")

    # 正样本
    pos_pairs = behavior_df[behavior_df["label"] == 1][["user_id", "house_id"]].values.tolist()
    # 负样本 (真实, 仅有 VIEW)
    real_neg_pairs = behavior_df[behavior_df["label"] == 0][["user_id", "house_id"]].values.tolist()
    print(f"   positives: {len(pos_pairs):,} | real negatives: {len(real_neg_pairs):,}")

    # 采样到 n_samples, 正负比 1 : neg_ratio
    n_pos_target = max(1000, int(n_samples / (1 + neg_ratio)))
    n_neg_target = n_samples - n_pos_target

    # 采样正样本 (with replacement if not enough)
    if len(pos_pairs) >= n_pos_target:
        pos_sampled = random.sample(pos_pairs, n_pos_target)
    else:
        pos_sampled = (pos_pairs * (n_pos_target // max(1, len(pos_pairs)) + 1))[:n_pos_target]

    # 负样本 = 真实负样本 + 随机负样本 (混合, 提高泛化)
    n_real_neg = min(len(real_neg_pairs), n_neg_target // 2)
    n_rand_neg = n_neg_target - n_real_neg
    real_neg_sampled = (random.sample(real_neg_pairs, n_real_neg)
                        if n_real_neg > 0 and len(real_neg_pairs) >= n_real_neg else [])
    # 随机负样本: 用 pos 样本里的 user 配随机 house
    pos_users = [p[0] for p in pos_sampled] if pos_sampled else \
                random.sample(range(1, 200001), min(10000, n_rand_neg))
    rand_neg_sampled = []
    for _ in range(n_rand_neg):
        u = random.choice(pos_users) if pos_users else random.randint(1, 200000)
        h = random.choice(houses_records)["house_id"]
        rand_neg_sampled.append([u, h])

    all_pairs = pos_sampled + real_neg_sampled + rand_neg_sampled
    all_labels = [1] * len(pos_sampled) + [0] * (len(real_neg_sampled) + len(rand_neg_sampled))

    # shuffle
    combined = list(zip(all_pairs, all_labels))
    random.shuffle(combined)
    all_pairs = [c[0] for c in combined]
    all_labels = [c[1] for c in combined]

    print(f"   sampled: {len(all_pairs):,} pairs (pos={sum(all_labels):,}, neg={len(all_pairs)-sum(all_labels):,})")

    # 构建特征
    X = np.zeros((len(all_pairs), N_FEATURES), dtype=np.float32)
    y = np.zeros((len(all_pairs),), dtype=np.float32)
    miss_user = 0
    for i, (uid, hid) in enumerate(all_pairs):
        house = house_lookup.get(hid)
        if house is None:
            # pick random house as fallback
            house = random.choice(houses_records)
        pref_row = user_prefs_db.get(uid)
        if pref_row is not None:
            pref = pref_from_db_row(pref_row)
        else:
            # Fallback: derive preference from house itself (assume same-city)
            pref = {"city": house.get("city"), "district": house.get("district"),
                    "rentType": house.get("rent_type"),
                    "min_price": None, "max_price": None, "room_count": None}
            miss_user += 1
        X[i] = build_features(pref, house)
        y[i] = all_labels[i]

    print(f"   features built. users without preference row (fallback used): {miss_user:,}")
    return X, y


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--mysql-host", default=os.environ.get("MYSQL_HOST", "192.168.24.129"))
    parser.add_argument("--mysql-port", type=int, default=int(os.environ.get("MYSQL_PORT", "3310")))
    parser.add_argument("--mysql-user", default=os.environ.get("MYSQL_USER", "root"))
    parser.add_argument("--mysql-pwd", default=os.environ.get("MYSQL_PWD", "123456"))
    parser.add_argument("--mysql-db", default=os.environ.get("MYSQL_DB", "apartment_db"))
    parser.add_argument("--epochs", type=int, default=20)
    parser.add_argument("--batch-size", type=int, default=512)
    parser.add_argument("--k-factors", type=int, default=8)
    parser.add_argument("--n-samples", type=int, default=80000)
    parser.add_argument("--label-source", choices=["synthetic", "real"], default="synthetic",
                        help="synthetic=rule-based(V1), real=user_behavior(V2)")
    parser.add_argument("--neg-ratio", type=float, default=3.0,
                        help="(real mode) negative-to-positive ratio")
    parser.add_argument("--out", default=os.path.join(os.path.dirname(os.path.abspath(__file__)), "fm-model-v1.json"))
    parser.add_argument("--metrics-out", default=None,
                        help="optional path to append training metrics JSON for comparison")
    args = parser.parse_args()

    os.environ["TF_CPP_MIN_LOG_LEVEL"] = "2"
    import tensorflow as tf
    tf.get_logger().setLevel("ERROR")

    print("=" * 60)
    print("  TensorFlow FM Recommender Training")
    print(f"  label-source = {args.label_source}")
    print("=" * 60)
    print("TensorFlow version:", tf.__version__)

    print("\n[1/5] loading houses from MySQL...")
    try:
        houses_df = load_houses_from_mysql(args.mysql_host, args.mysql_port,
                                           args.mysql_user, args.mysql_pwd,
                                           args.mysql_db)
        print("   OK loaded", len(houses_df), "houses")
    except Exception as e:
        print("   WARN MySQL connect failed:", e, "use synthetic demo data")
        houses_df = pd.DataFrame([
            {"house_id": i, "city": random.choice(["北京市", "上海市"]),
             "district": "d" + str(random.randint(1, 5)),
             "rent_type": random.choice(["WHOLE", "SHARED"]),
             "price": random.randint(2000, 9000),
             "room_count": random.choice([1, 2, 3]),
             "view_count": random.randint(0, 500),
             "status": "AVAILABLE",
             "create_time": datetime.now() - timedelta(days=random.randint(0, 90))}
            for i in range(2000)
        ])

    houses = houses_df.to_dict("records")

    print(f"\n[2/5] building dataset (n_samples={args.n_samples}, label_source={args.label_source})...")
    random.seed(42)
    np.random.seed(42)
    tf.random.set_seed(42)

    if args.label_source == "synthetic":
        prefs = gen_predefined_preferences()
        cities = houses_df["city"].dropna().unique().tolist()[:10]
        prefs += gen_random_preferences(max(50, args.n_samples // 2000), cities)

        X = np.zeros((args.n_samples, N_FEATURES), dtype=np.float32)
        y = np.zeros((args.n_samples,), dtype=np.float32)
        for i in range(args.n_samples):
            pref = random.choice(prefs)
            house = random.choice(houses)
            X[i] = build_features(pref, house)
            y[i] = synthetic_label(pref, house)
        dataset_meta = {"source": "synthetic", "neg_ratio": None}
    else:
        # V2: real behavior labels
        print("   loading user_preference table...")
        user_prefs_db = load_user_preferences_from_db(
            args.mysql_host, args.mysql_port, args.mysql_user,
            args.mysql_pwd, args.mysql_db)
        print(f"   loaded {len(user_prefs_db):,} user preference rows")

        print("   loading user_behavior aggregates...")
        behavior_df = load_behavior_labels_from_db(
            args.mysql_host, args.mysql_port, args.mysql_user,
            args.mysql_pwd, args.mysql_db)
        print(f"   loaded {len(behavior_df):,} (user, house) pairs "
              f"(pos={behavior_df['label'].sum():,}, "
              f"neg={len(behavior_df) - behavior_df['label'].sum():,})")

        X, y = build_real_label_dataset(
            houses_df, behavior_df, user_prefs_db,
            n_samples=args.n_samples, neg_ratio=args.neg_ratio,
            random_seed=42)
        dataset_meta = {"source": "real", "neg_ratio": args.neg_ratio,
                        "total_pairs": int(len(behavior_df))}

    pos_rate = y.mean()
    print(f"   OK positive ratio: {pos_rate:.4f}")

    from sklearn.model_selection import train_test_split
    X_train, X_val, y_train, y_val = train_test_split(X, y, test_size=0.15, random_state=42)
    print("   OK train", len(X_train), "/ val", len(X_val))

    print("\n[3/5] building FM model (k=" + str(args.k_factors) + ")...")
    model = build_fm_model(N_FEATURES, k=args.k_factors)
    model.compile(optimizer=tf.keras.optimizers.Adam(learning_rate=0.01),
                  loss="binary_crossentropy",
                  metrics=[tf.keras.metrics.AUC(name="auc"), "accuracy"])
    model.summary()

    print("\n[4/5] training", args.epochs, "epochs...")
    early_stop = tf.keras.callbacks.EarlyStopping(
        monitor="val_auc", mode="max", patience=4, restore_best_weights=True)
    history = model.fit(X_train, y_train,
                        validation_data=(X_val, y_val),
                        epochs=args.epochs,
                        batch_size=args.batch_size,
                        callbacks=[early_stop],
                        verbose=2)
    val_auc = max(history.history.get("val_auc", [0]))
    val_acc = max(history.history.get("val_accuracy", [0]))
    print("   OK best val_auc=" + str(round(val_auc, 4)) + ", val_acc=" + str(round(val_acc, 4)))

    print("\n[5/5] exporting Java-compatible FM model to", args.out)
    weights = {w.name.split(":")[0]: w.numpy() for w in model.weights}
    bias = float(weights["fm/bias"][0])
    linear_w = weights["fm/linear"]
    factors = weights["fm/factors"]

    # Determine version tag from filename
    out_basename = os.path.basename(args.out).replace(".json", "")
    version_tag = out_basename if out_basename.startswith("tf-fm") else f"tf-{out_basename}"

    export = {
        "metadata": {
            "version": version_tag,
            "labelSource": args.label_source,
            "trainedAt": datetime.now().isoformat(),
            "tensorflowVersion": tf.__version__,
            "nFeatures": N_FEATURES,
            "kFactors": int(args.k_factors),
            "nSamples": int(args.n_samples),
            "negRatio": float(args.neg_ratio) if args.label_source == "real" else None,
            "posRate": float(round(pos_rate, 4)),
            "valAuc": float(round(val_auc, 4)),
            "valAccuracy": float(round(val_acc, 4)),
            "datasetMeta": dataset_meta,
        },
        "bias": round(bias, 6),
        "linearWeights": {FEATURE_NAMES[i]: round(float(linear_w[i]), 6) for i in range(N_FEATURES)},
        "factors": {FEATURE_NAMES[i]: [round(float(x), 6) for x in factors[i]]
                    for i in range(N_FEATURES)},
    }
    with open(args.out, "w", encoding="utf-8") as f:
        json.dump(export, f, ensure_ascii=False, indent=2)
    print("   OK exported. bias=" + str(round(export["bias"], 4)))

    # Append metrics for comparison report
    if args.metrics_out:
        metrics_entry = {
            "labelSource": args.label_source,
            "nSamples": int(args.n_samples),
            "kFactors": int(args.k_factors),
            "epochs": int(args.epochs),
            "batchSize": int(args.batch_size),
            "negRatio": float(args.neg_ratio) if args.label_source == "real" else None,
            "posRate": float(round(pos_rate, 4)),
            "valAuc": float(round(val_auc, 4)),
            "valAccuracy": float(round(val_acc, 4)),
            "trainedAt": datetime.now().isoformat(),
            "history": {
                "trainLoss": [round(float(x), 4) for x in history.history.get("loss", [])],
                "valLoss": [round(float(x), 4) for x in history.history.get("val_loss", [])],
                "trainAuc": [round(float(x), 4) for x in history.history.get("auc", [])],
                "valAuc": [round(float(x), 4) for x in history.history.get("val_auc", [])],
                "trainAcc": [round(float(x), 4) for x in history.history.get("accuracy", [])],
                "valAcc": [round(float(x), 4) for x in history.history.get("val_accuracy", [])],
            },
        }
        # append to list
        existing = []
        if os.path.exists(args.metrics_out):
            try:
                with open(args.metrics_out, "r", encoding="utf-8") as f:
                    existing = json.load(f)
                    if not isinstance(existing, list):
                        existing = [existing]
            except Exception:
                existing = []
        existing.append(metrics_entry)
        with open(args.metrics_out, "w", encoding="utf-8") as f:
            json.dump(existing, f, ensure_ascii=False, indent=2)
        print(f"   metrics appended to {args.metrics_out}")

    print("\n[Top5 Feature Importance]")
    importance = sorted(zip(FEATURE_NAMES, linear_w), key=lambda kv: abs(kv[1]), reverse=True)
    for name, w in importance[:5]:
        print("   " + name.ljust(24) + " w=" + str(round(float(w), 4)))

    print("\n" + "=" * 60)
    print(f"  DONE! val_auc={val_auc:.4f}, val_acc={val_acc:.4f}")
    print(f"  Model: {args.out}")
    print("=" * 60)


if __name__ == "__main__":
    main()
