# Day5 Linux 常用命令与线上排查流程 核心知识点背诵笔记 ⭐⭐⭐

---

## 一、CPU 飙高排查 SOP

```
① top                          → 找 CPU 最高的 Java 进程 PID
② top -Hp <pid>                → 找进程中 CPU 最高的线程 TID
③ printf '%x\n' <tid>          → 线程 ID 转 16 进制
④ jstack <pid> | grep -A 30 <nid> → 定位线程栈 → 看是哪段代码
⑤ 判断线程类型：
   - GC 线程 → jstat -gcutil 确认 → 调 GC 参数或排查内存泄漏
   - 业务线程 → 代码热循环 / 死循环 → 修代码
   - VM Thread → 考虑 dump 堆分析
```

---

## 二、内存溢出排查 SOP

```
① jmap -dump:format=b,file=heap.hprof <pid>    → 导出堆 dump
② MAT（Memory Analyzer Tool）打开 dump
   - Leak Suspects Report  → 自动分析泄漏嫌疑
   - Dominator Tree        → 找到占用最大的对象
   - GC Roots 追溯         → 找到谁在引用它
③ 定位根因：
   - 堆溢出 → 调大 -Xmx / 优化代码减少对象创建
   - 元空间溢出 → -XX:MaxMetaspaceSize / 排查 CGLIB 动态类
```

---

## 三、死锁排查

```bash
jstack <pid> | grep -A 50 "deadlock"
# 直接看 Found one Java-level deadlock
# 会列出死锁的线程、持有的锁、等待的锁
```

---

## 四、常用命令速查

### 4.1 进程和端口

| 命令 | 作用 |
|---|---|
| `jps -lvm` | 查看 Java 进程 + JVM 参数 |
| `ps -ef \| grep java` | 查看 Java 进程详情 |
| `netstat -tlnp` | 监听端口列表 |
| `lsof -i :8080` | 查谁在用 8080 端口 |
| `ss -tlnp` | 更快的 netstat |

### 4.2 磁盘和内存

| 命令 | 作用 |
|---|---|
| `top` / `htop` | CPU/内存排序（P=CPU, M=Memory） |
| `free -h` | 内存使用概览 |
| `df -h` | 磁盘使用 |
| `iostat -x 1` | 磁盘 IO 实时 |
| `vmstat 1` | 系统整体（CPU/内存/IO/swap） |

### 4.3 Java 专属

| 命令 | 作用 |
|---|---|
| `jstat -gcutil <pid> 1000` | 每秒看 GC 数据 |
| `jmap -histo <pid>` | 对象直方图 |
| `jmap -dump:format=b,file=xxx.hprof <pid>` | 堆 dump |
| `jstack <pid>` | 线程栈 |
| `jinfo <pid>` | JVM 运行时参数 |
| `jcmd <pid> help` | 查看可用命令 |

### 4.4 日志

| 命令 | 作用 |
|---|---|
| `tail -f -n 200 app.log` | 实时滚动最新 200 行 |
| `grep "ERROR" app.log \| tail -20` | 过滤最新错误 |
| `less app.log` | 大文件浏览（/搜索, ?向上搜索） |
| `sed -n '/start_time/,/end_time/p' app.log` | 按时间段查日志 |

---

## 五、线上排查思维流程

```
接口变慢/报错
  │
  ├→ 先看监控：CPU？内存？GC？QPS？接口耗时？
  │
  ├→ 看日志：错误堆栈？（grep ERROR）
  │
  ├→ 查数据库：慢查询？（explain / 索引）
  │
  ├→ 查缓存：Redis 是否正常？key 是否失效？
  │
  ├→ 查下游：MQ 是否积压？下游服务是否异常？
  │
  └→ 查 JVM：GC 频繁？（jstat）/ 内存泄漏？（jmap+MAT）
```

---

## 六、终极背诵总结

1. **CPU 高**：top → top -Hp → 16进制 → jstack → 定位代码
2. **OOM**：jmap dump → MAT → Leak Suspects → GC Roots
3. **死锁**：jstack \| grep deadlock
4. **jstat -gcutil**：生产铁律 FGC 必须为 0
5. **日志三板斧**：tail -f 看实时、grep ERROR 找错、less 大文件搜索
