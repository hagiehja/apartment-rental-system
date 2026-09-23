#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
公寓系统真实分布数据生成器
分布参数参考:贝壳找房 2024 北京租赁报告 / 国家统计局 / 公开市场数据
输出 UTF-8 CSV,供 MySQL LOAD DATA INFILE 导入
用法: python3 gen_realistic.py <用户数> <房源数> <订单数> <支付数>
"""
import random, csv, sys, os
from datetime import datetime, timedelta

random.seed(42)

# ============ 真实分布参数(公开数据建模)============
# 北京各区: (区名, 租金均值, σ, 占比)
DISTRICTS = [
    ('朝阳区', 8200, 2000, 0.25),
    ('海淀区', 7800, 1800, 0.18),
    ('丰台区', 6800, 1500, 0.12),
    ('通州区', 4800, 1200, 0.10),
    ('昌平区', 5200, 1300, 0.10),
    ('西城区', 9500, 2200, 0.08),
    ('东城区', 8500, 2000, 0.07),
    ('石景山区', 6500, 1400, 0.05),
    ('大兴区', 5000, 1200, 0.05),
]
ORIENTATIONS = ['南', '北', '东南', '南北', '西南', '东西']
DECORATIONS = ['精装', '精装', '简装', '豪装', '毛坯']  # 精装权重高
HOUSE_TYPES = [('一居室',0.18,1,55,10), ('两居室',0.38,2,80,12),
               ('三居室',0.32,3,110,18), ('四居室',0.12,4,150,25)]
ORDER_STATUS = [('ACTIVE',0.30),('COMPLETED',0.35),('PENDING_PAYMENT',0.12),
                ('CANCELLED',0.18),('EXPIRED',0.05)]
PAY_METHODS = [('BALANCE',0.35),('ALIPAY',0.38),('WECHAT',0.27)]
PAY_STATUS = [('SUCCESS',0.78),('PENDING',0.10),('FAILED',0.07),('REFUNDED',0.05)]
BCRYPT_HASH = '$2b$10$Y2c85Ceb/V4vs8.xQ1kn3u/PpR2BBIDo6ndgCyqhYso.XxnnfuYgC'  # 123456

def wchoice(items):
    r = random.random(); cum = 0
    for val, w in items:
        cum += w
        if r <= cum: return val
    return items[-1][0]

def phone(i):
    return random.choice(['138','139','135','136','151','152','158','159','176','178','180','182','186','188','189']) + str(i % 100000000).zfill(8)

def gen_users(n, fn):
    print(f'[1/4] 生成 {n:,} 用户...', flush=True)
    roles = [('TENANT',0.70),('LANDLORD',0.27),('ADMIN',0.03)]
    with open(fn,'w',encoding='utf-8',newline='') as f:
        w = csv.writer(f)
        for i in range(1, n+1):
            t = (datetime.now()-timedelta(days=random.randint(0,730))).strftime('%Y-%m-%d %H:%M:%S')
            w.writerow([f'user_{i:07d}', phone(i), BCRYPT_HASH, wchoice(roles), t, t])
            if i % 200000 == 0: print(f'  用户 {i:,}/{n:,}', flush=True)

def gen_houses(n, nusers, fn):
    print(f'[2/4] 生成 {n:,} 房源...', flush=True)
    with open(fn,'w',encoding='utf-8',newline='') as f:
        w = csv.writer(f)
        for i in range(1, n+1):
            dname = wchoice([(d[0],d[3]) for d in DISTRICTS])
            di = next(d for d in DISTRICTS if d[0]==dname)
            ht = wchoice([(t[0],t[1]) for t in HOUSE_TYPES])
            ti = next(t for t in HOUSE_TYPES if t[0]==ht)
            area = max(20, round(random.gauss(ti[3], ti[4]), 2))
            price = max(1500, round(random.gauss(di[1], di[2]), 2))
            title = f'{dname}{ht} 精装修 靠近地铁 拎包入住'
            lid = random.randint(1, nusers)
            t = (datetime.now()-timedelta(days=random.randint(0,730))).strftime('%Y-%m-%d %H:%M:%S')
            w.writerow([lid, title, f'{title}，配套齐全，交通便利',
                '北京','北京市',dname, f'{dname}路{random.randint(1,999)}号',
                area, ti[2], random.randint(1,2), random.randint(1,2),
                random.randint(1,30), random.randint(6,33), random.choice(ORIENTATIONS),
                random.choice(DECORATIONS), wchoice([('WHOLE',0.65),('SHARED',0.35)]),
                price, random.choice(['月付','季付','年付']),
                '空调,洗衣机,冰箱,热水器,宽带,天然气',
                wchoice([('AVAILABLE',0.60),('RENTED',0.30),('OFFLINE',0.10)]),
                int(random.expovariate(1/500)), t, t, 0])
            if i % 200000 == 0: print(f'  房源 {i:,}/{n:,}', flush=True)

def gen_orders(n, nusers, nhouses, fn):
    print(f'[3/4] 生成 {n:,} 订单...', flush=True)
    with open(fn,'w',encoding='utf-8',newline='') as f:
        w = csv.writer(f)
        for i in range(1, n+1):
            rent = round(max(1500, random.gauss(6500, 2500)), 2)
            months = random.choices([1,3,6,12], weights=[20,40,25,15])[0]
            dep = round(rent*2, 2)
            status = wchoice(ORDER_STATUS)
            pstat = 'PAID' if status in ('ACTIVE','COMPLETED') else ('REFUNDED' if status=='CANCELLED' and random.random()<0.3 else 'UNPAID')
            days = min(int(random.expovariate(1/365)), 730)
            base = datetime.now()-timedelta(days=days)
            rs = (base+timedelta(days=random.randint(0,30))).strftime('%Y-%m-%d')
            re = (datetime.strptime(rs,'%Y-%m-%d')+timedelta(days=30*months)).strftime('%Y-%m-%d')
            ct = base.strftime('%Y-%m-%d %H:%M:%S')
            pt = (base+timedelta(hours=random.randint(0,12))).strftime('%Y-%m-%d %H:%M:%S') if pstat=='PAID' else None
            w.writerow([f'ORD{days:04d}{i:09d}', random.randint(1,nusers), random.randint(1,nhouses),
                random.randint(1,nusers), rs, re, months, rent, dep, round(rent*months+dep,2),
                round(rent+dep,2), 0, status, pstat,
                (base+timedelta(hours=24)).strftime('%Y-%m-%d %H:%M:%S'), ct, pt])
            if i % 500000 == 0: print(f'  订单 {i:,}/{n:,}', flush=True)

def gen_payments(n, nusers, fn):
    print(f'[4/4] 生成 {n:,} 支付...', flush=True)
    with open(fn,'w',encoding='utf-8',newline='') as f:
        w = csv.writer(f)
        for i in range(1, n+1):
            amt = round(max(1500, random.gauss(8000, 4000)), 2)
            status = wchoice(PAY_STATUS)
            t = (datetime.now()-timedelta(days=random.randint(0,730))).strftime('%Y-%m-%d %H:%M:%S')
            w.writerow([f'PAY{i:013d}', f'ORD{i:013d}', random.randint(1,nusers), amt,
                wchoice(PAY_METHODS), status, amt if status=='REFUNDED' else 0,
                t if status in ('SUCCESS','REFUNDED') else None, t, t])
            if i % 500000 == 0: print(f'  支付 {i:,}/{n:,}', flush=True)

if __name__ == '__main__':
    nu = int(sys.argv[1]) if len(sys.argv)>1 else 10000
    nh = int(sys.argv[2]) if len(sys.argv)>2 else 5000
    no = int(sys.argv[3]) if len(sys.argv)>3 else 10000
    np_ = int(sys.argv[4]) if len(sys.argv)>4 else 5000
    out = '/tmp'
    gen_users(nu, f'{out}/users.csv')
    gen_houses(nh, nu, f'{out}/houses.csv')
    gen_orders(no, nu, nh, f'{out}/orders.csv')
    gen_payments(np_, nu, f'{out}/payments.csv')
    print('=== CSV 全部生成完成 ===', flush=True)
    for fn in ['users','houses','orders','payments']:
        sz = os.path.getsize(f'{out}/{fn}.csv')
        print(f'  {fn}.csv: {sz/1024/1024:.1f} MB', flush=True)
