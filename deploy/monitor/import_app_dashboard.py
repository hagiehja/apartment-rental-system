#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""导入应用层 Dashboard(JVM + HTTP 指标),构建完成后运行"""
import urllib.request, urllib.parse, json, base64, sys
GRAFANA = "http://localhost:3000"
AUTH = "Basic " + base64.b64encode(b"admin:admin").decode()

def get_ds_uid():
    req = urllib.request.Request(f"{GRAFANA}/api/datasources/name/Prometheus", headers={"Authorization":AUTH})
    return json.loads(urllib.request.urlopen(req).read())["uid"]

DS_UID = get_ds_uid()
DS = {"type":"prometheus","uid":DS_UID}

panels = [
    # Row1: 关键 stat
    {"type":"stat","title":"order-service JVM 堆使用","datasource":DS,
     "targets":[{"expr":'sum(jvm_memory_used_bytes{area="heap",application="apartment-order-service"})'}],
     "gridPos":{"h":6,"w":6,"x":0,"y":0},"fieldConfig":{"defaults":{"unit":"bytes"}}},
    {"type":"stat","title":"各服务进程 CPU","datasource":DS,
     "targets":[{"expr":'avg(process_cpu_usage) by (application)'}],
     "gridPos":{"h":6,"w":6,"x":6,"y":0},"fieldConfig":{"defaults":{"unit":"percentunit"}}},
    {"type":"stat","title":"HTTP 请求速率(QPS)","datasource":DS,
     "targets":[{"expr":'sum(rate(http_server_requests_seconds_count[1m]))'}],
     "gridPos":{"h":6,"w":6,"x":12,"y":0},"fieldConfig":{"defaults":{"unit":"reqps"}}},
    {"type":"stat","title":"order 实例数(弹性)","datasource":DS,
     "targets":[{"expr":'count(jvm_memory_used_bytes{application="apartment-order-service"}) by (application) or vector(0)'}],
     "gridPos":{"h":6,"w":6,"x":18,"y":0},"fieldConfig":{"defaults":{"unit":"short"}}},
    # Row2: JVM 内存
    {"type":"timeseries","title":"各服务 JVM 堆内存使用","datasource":DS,
     "targets":[{"expr":'sum(jvm_memory_used_bytes{area="heap"}) by (application)',"legendFormat":"{{application}}"}],
     "gridPos":{"h":8,"w":12,"x":0,"y":6},"fieldConfig":{"defaults":{"unit":"bytes"}}},
    {"type":"timeseries","title":"各服务 JVM GC 暂停速率","datasource":DS,
     "targets":[{"expr":'sum(rate(jvm_gc_pause_seconds_sum[5m])) by (application)',"legendFormat":"{{application}}"}],
     "gridPos":{"h":8,"w":12,"x":12,"y":6},"fieldConfig":{"defaults":{"unit":"s"}}},
    # Row3: HTTP
    {"type":"timeseries","title":"HTTP 请求 QPS(按服务)","datasource":DS,
     "targets":[{"expr":'sum(rate(http_server_requests_seconds_count[1m])) by (application)',"legendFormat":"{{application}}"}],
     "gridPos":{"h":8,"w":12,"x":0,"y":14},"fieldConfig":{"defaults":{"unit":"reqps"}}},
    {"type":"timeseries","title":"HTTP 响应时间 P95(按服务)","datasource":DS,
     "targets":[{"expr":'histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))',"legendFormat":"{{application}}"}],
     "gridPos":{"h":8,"w":12,"x":12,"y":14},"fieldConfig":{"defaults":{"unit":"s"}}},
    # Row4: 线程 + 实例数
    {"type":"timeseries","title":"各服务 JVM 线程数","datasource":DS,
     "targets":[{"expr":'jvm_threads_states_threads',"legendFormat":"{{application}} {{state}}"}],
     "gridPos":{"h":8,"w":12,"x":0,"y":22},"fieldConfig":{"defaults":{"unit":"short"}}},
    {"type":"timeseries","title":"各服务实例数(弹性伸缩监控)","datasource":DS,
     "targets":[{"expr":'count(up{job="apartment-apps"}) by (instance)',"legendFormat":"{{instance}}"}],
     "gridPos":{"h":8,"w":12,"x":12,"y":22},"fieldConfig":{"defaults":{"unit":"short"}}},
]

dashboard = {"dashboard":{
    "id":None,"uid":"apartment-app",
    "title":"🚀 Apartment 应用层监控(JVM/HTTP/弹性)",
    "timezone":"browser","schemaVersion":39,"refresh":"10s","tags":["apartment","L1","app"],
    "time":{"from":"now-30m","to":"now"},
    "panels":panels},
    "folderId":0,"overwrite":True}

req = urllib.request.Request(f"{GRAFANA}/api/dashboards/db",
    data=json.dumps(dashboard).encode(),
    headers={"Content-Type":"application/json","Authorization":AUTH})
r = json.loads(urllib.request.urlopen(req).read())
print("导入结果:", r.get("status"), "url=/d/"+r.get("uid",""))
