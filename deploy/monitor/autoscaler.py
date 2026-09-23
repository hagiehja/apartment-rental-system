#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Apartment 系统弹性伸缩器 (L1 轨道C)
基于 Prometheus 采集的容器 CPU,自动 scale up/down 微服务实例
用法:
  python3 autoscaler.py           # 守护循环(每 INTERVAL 秒决策)
  python3 autoscaler.py --once    # 单次决策(测试用)
  python3 autoscaler.py --demo    # 低阈值演示模式(易触发)
"""
import urllib.request, urllib.parse, json, subprocess, time, os, re, sys
from datetime import datetime

PROM      = os.environ.get("PROMETHEUS", "http://localhost:9090")
DIR       = "/home/hyz/apartment-rental-system"
FILE      = "docker-compose.ha.yml"
PROJECT   = "apartment-ha"
INTERVAL  = int(os.environ.get("INTERVAL", "20"))

# 正常策略
NORMAL = {
    "apartment-order-service": {"min":1,"max":3,"up":55,"down":15},
    "apartment-user-service":  {"min":1,"max":2,"up":55,"down":15},
    "apartment-gateway":       {"min":1,"max":2,"up":65,"down":15},
}
# 演示策略(低阈值,易触发)
DEMO = {
    "apartment-order-service": {"min":1,"max":3,"up":3,"down":1},
    "apartment-user-service":  {"min":1,"max":2,"up":3,"down":1},
}
DEF   = {"min":1,"max":1,"up":70,"down":15}

CPU_Q = ('avg by (name) (rate(container_cpu_usage_seconds_total'
         '{container_label_com_docker_compose_project="apartment-ha"}[2m])) * 100')

def log(m):
    print(f"[{datetime.now():%H:%M:%S}] {m}", flush=True)

def query_cpu():
    try:
        url = f"{PROM}/api/v1/query?query={urllib.parse.quote(CPU_Q)}"
        d = json.loads(urllib.request.urlopen(url, timeout=8).read())
        return {it["metric"].get("name","?"): float(it["value"][1])
                for it in d["data"]["result"]}
    except Exception as e:
        log(f"[ERR] 查询失败: {e}"); return {}

def aggregate(cpus):
    """容器名 → 服务名,取平均"""
    svc = {}
    for name, cpu in cpus.items():
        m = re.search(r'(apartment-[a-z]+-service|apartment-gateway)', name)
        if m:
            s = m.group(1)
            svc.setdefault(s, []).append(cpu)
    return {s: round(sum(v)/len(v),2) for s,v in svc.items()}

def replicas():
    try:
        out = subprocess.check_output(
            ["docker","ps","--filter",f"name={PROJECT}-apartment","--format","{{.Names}}"],
            timeout=10).decode()
        c = {}
        for ln in out.splitlines():
            m = re.search(r'(apartment-[a-z]+-service|apartment-gateway)-\d+', ln)
            if m: c[m.group(1)] = c.get(m.group(1),0)+1
        return c
    except Exception as e:
        log(f"[ERR] docker ps: {e}"); return {}

def do_scale(svc, n):
    log(f"  >>> docker compose scale {svc}={n}")
    subprocess.run(["docker","compose","-p",PROJECT,"-f",FILE,"up","-d",
                    "--scale",f"{svc}={n}","--no-deps",svc],
                   cwd=DIR, capture_output=True, timeout=120)

def decide(policies):
    raw = query_cpu()
    svc_cpu = aggregate(raw)
    cur = replicas()
    log(f"容器CPU%: {svc_cpu}")
    log(f"实例数:   {cur}")
    actions = 0
    for svc, p in policies.items():
        n = cur.get(svc, 0)
        cpu = svc_cpu.get(svc, 0)
        if n == 0: continue
        if cpu > p["up"] and n < p["max"]:
            log(f"⚡ {svc} CPU={cpu}%>{p['up']}% → 扩容 {n}→{n+1}")
            do_scale(svc, n+1); actions += 1
        elif cpu < p["down"] and n > p["min"]:
            log(f"💧 {svc} CPU={cpu}%<{p['down']}% → 缩容 {n}→{n-1}")
            do_scale(svc, n-1); actions += 1
        else:
            log(f"   {svc}: CPU={cpu}% 实例={n} (稳定)")
    # 未纳管服务
    for svc in cur:
        if svc not in policies:
            log(f"   {svc}: 实例={cur[svc]} (未纳管,跳过)")
    log(f"本轮决策完成,执行 {actions} 次伸缩")
    return actions

def main():
    demo = "--demo" in sys.argv
    once = "--once" in sys.argv
    pol = DEMO if demo else NORMAL
    log(f"=== 弹性伸缩器启动 ({'演示' if demo else '正常'}模式, 间隔{INTERVAL}s) ===")
    log(f"策略: {pol}")
    while True:
        try: decide(pol)
        except Exception as e: log(f"[ERR] {e}")
        if once: break
        time.sleep(INTERVAL)

if __name__ == "__main__":
    main()
