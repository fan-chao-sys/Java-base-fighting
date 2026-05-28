# Day5 CMS、G1、GC 日志与参数 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、CMS（Concurrent Mark Sweep）—— 老年代并发收集器

### 1.1 CMS 四阶段（必须按顺序记住）

```
┌─────────────────────────────────────────────────────────────────┐
│                    CMS 一次 GC 的完整时序                          │
├────────┬────────────┬────────────┬────────────┬────────────────┤
│ 初始标记 │  并发标记    │  重新标记   │  并发清除   │                │
│ (STW)  │ (并发,不STW) │  (STW)    │(并发,不STW) │                │
│ 短暂停  │  长时间      │  中暂停    │  长时间     │                │
└────────┴────────────┴────────────┴────────────┴────────────────┘
```

#### ① 初始标记（Initial Mark）—— STW

- **仅标记 GC Roots 直接关联的对象**（一层引用）
- 停顿时间**极短**（毫秒级）
- 需要 STW，因为要获取一个一致性快照

#### ② 并发标记（Concurrent Mark）—— 与用户线程并发

- 从 GC Roots 出发，**沿引用链遍历整个对象图**
- 与用户线程**并发执行**，不 STW
- 耗时最长，但用户无感知
- **问题**：期间用户线程可能产生新垃圾（浮动垃圾）+ 修改引用

#### ③ 重新标记（Remark）—— STW

- **修正并发标记期间引用变化**的那些对象
- 只扫描并发标记期间被修改的少量对象
- 停顿比初始标记长，但比完全 STW 短
- 使用**增量更新 + 写屏障**记录变化

#### ④ 并发清除（Concurrent Sweep）—— 与用户线程并发

- 清理未标记的垃圾对象
- 与用户线程并发，不需要移动对象 → **产生碎片**

### 1.2 CMS 的三大缺点

| 缺点 | 具体表现 | 影响 |
|---|---|---|
| **CPU 敏感** | 并发阶段占用 CPU 线程（默认 (核数+3)/4 个线程） | 核数少时吞吐量下降 |
| **浮动垃圾** | 并发标记后用户线程新产生垃圾 → 本次 GC 无法回收 → 只能等下次 | 需要预留老年代空间 |
| **碎片化** | 基于标记-清除，不整理 → 碎片积累 → 大对象分配失败 | 碎片严重时退化用 Serial Old（全 STW） |

### 1.3 Concurrent Mode Failure ⭐

CMS 并发清理阶段，老年代剩余空间 < Young GC 晋升对象所需 → **失败**。

**后果**：CMS 回退用 **Serial Old** 做一次完全 STW 的 Full GC → **停顿时间暴增**。

**触发条件**：
- 老年代使用率超过 `-XX:CMSInitiatingOccupancyFraction`（默认 92%）
- 浮动垃圾太多，预留空间不够

**解决**：
- 调低 CMSInitiatingOccupancyFraction（如 70，给浮动垃圾预留 30%）
- 加大老年代 / 整体堆

### 1.4 CMS 状态

- JDK 9 → 标记为 **Deprecated**
- JDK 14 → **正式移除**

---

## 二、G1（Garbage First）—— 现代默认 GC ⭐⭐⭐

### 2.1 G1 核心创新：Region 设计

```
传统分代：                        G1 的 Region 布局：
┌────────────┐                   ┌──┬──┬──┬──┬──┬──┐
│            │                   │E │S │O │E │H │O │  每个 Region 大小相同
│   老年代    │                   ├──┼──┼──┼──┼──┼──┤  默认 2048 个 Region
│            │                   │E │O │E │O │E │S │  -XX:G1HeapRegionSize
├────────────┤                   ├──┼──┼──┼──┼──┼──┤
│ S0│S1│Eden│                   │O │E │O │E │O │E │  E=Eden, S=Survivor
└────────────┘                   └──┴──┴──┴──┴──┴──┘  O=Old, H=Humongous(大对象)
```

**Region 类型**：
- **Eden**：年轻代
- **Survivor**：存活区
- **Old**：老年代
- **Humongous**：巨型对象（≥ 半个 Region 大小），单独连续 Region 存储

### 2.2 G1 回收阶段

```
1. Young GC（年轻代回收）
   Eden 满 → 复制存活对象到 Survivor/Old → 回收 Eden Region

2. Mixed GC（混合回收）⭐ G1 独有
   回收全部年轻代 + **部分**老年代 Region（选回收价值最高的）
   → 基于"垃圾优先"：先回收垃圾最多的 Region

3. Full GC
   当复制/晋升失败（Evacuation Failure）→ 退化用 Serial Old
```

### 2.3 G1 Mixed GC 的筛选回收

```
老年代 100 个 Region：
  Region 1: 90% 垃圾 → 回收收益极高 ✓
  Region 2: 85% 垃圾 → 回收收益高   ✓
  Region 3: 20% 垃圾 → 回收收益低   ✗（本次跳过）
  ...

每次 Mixed GC 在 -XX:MaxGCPauseMillis（默认 200ms）内
选择回收价值最高的 N 个 Region
```

**核心公式**：回收收益 = 可回收空间 / 回收耗时

---

## 三、CMS vs G1 终极对比

| 对比维度 | CMS | G1 |
|---|---|---|
| **算法** | 老年代：标记-清除 | 整体：标记-整理 + 复制 |
| **内存布局** | 物理分年轻代/老年代 | **Region 化**，不物理隔离 |
| **碎片化** | **有**（标记-清除不清碎片） | **无**（使用复制/整理） |
| **可预测停顿** | 不可以 | **可以**（`-XX:MaxGCPauseMillis`） |
| **回收粒度** | 全堆或全老年代 | 部分 Region（垃圾最多的优先） |
| **适用堆大小** | ≤ 4-8G | **≥ 6G**（大堆优势明显） |
| **默认** | — | **JDK 9+ 默认** |
| **状态** | JDK 14 移除 | 当前主力 |

---

## 四、常用 JVM 参数速查表

### 4.1 堆参数

| 参数 | 含义 | 示例 |
|---|---|---|
| `-Xms` | 堆初始大小 | `-Xms2g` |
| `-Xmx` | 堆最大大小 | `-Xmx4g`（**Xms 和 Xmx 设置一样，避免堆动态扩缩开销**） |
| `-Xmn` | 年轻代大小 | `-Xmn1g` |
| `-XX:NewRatio` | 老年代/年轻代比例 | `-XX:NewRatio=2`（默认 2，年轻代占 1/3） |
| `-XX:SurvivorRatio` | Eden/Survivor 比例 | `-XX:SurvivorRatio=8`（默认 8:1:1） |

### 4.2 GC 选择参数

| 参数 | 含义 |
|---|---|
| `-XX:+UseG1GC` | 使用 G1（JDK 9+ 默认） |
| `-XX:+UseConcMarkSweepGC` | 使用 CMS（JDK 14 已移除） |
| `-XX:+UseParallelGC` | 吞吐量优先（Parallel Scavenge + Parallel Old） |
| `-XX:+UseSerialGC` | 单线程（Client 模式 / 小堆） |

### 4.3 GC 日志参数（JDK 8 / JDK 9+）

| JDK 版本 | 参数 |
|---|---|
| **JDK 8** | `-XX:+PrintGCDetails -XX:+PrintGCDateStamps -Xloggc:/path/gc.log` |
| **JDK 9+** | `-Xlog:gc*:file=/path/gc.log:time,level,tags` |

### 4.4 G1 特定参数

| 参数 | 含义 | 默认值 |
|---|---|---|
| `-XX:MaxGCPauseMillis` | 期望最大停顿时间（软目标） | 200ms |
| `-XX:G1HeapRegionSize` | Region 大小（1MB~32MB） | 堆/2048 自动算 |
| `-XX:InitiatingHeapOccupancyPercent` | 触发 Mixed GC 的堆占用阈值 | 45% |
| `-XX:G1MixedGCCountTarget` | Mixed GC 分几次完成 | 8 |
| `-XX:G1ReservePercent` | 预留空间防止晋升失败 | 10% |

### 4.5 OOM 排查参数

| 参数 | 含义 |
|---|---|
| `-XX:+HeapDumpOnOutOfMemoryError` | OOM 时自动生成 heap dump |
| `-XX:HeapDumpPath=/path/dump.hprof` | dump 文件路径 |
| `-XX:OnOutOfMemoryError=脚本` | OOM 时执行指定脚本（如自动重启） |
| `-XX:+PrintGCApplicationStoppedTime` | 打印 STW 停顿时间 |

---

## 五、GC 日志解读（G1 示例）

```
[GC pause (G1 Evacuation Pause) (young), 0.0058239 secs]
   [Parallel Time: 5.6 ms, GC Workers: 4]    ← 4 个 GC 线程并行
      [GC Worker Start: ...]
      [Ext Root Scanning: 0.2 ms]             ← 扫描 GC Roots
      [Update RS: 1.8 ms]                     ← 更新 Remember Set
      [Object Copy: 2.1 ms]                   ← 复制存活对象
      [Termination: 0.3 ms]
   [Eden: 512.0M(512.0M)->0.0B(512.0M)]      ← Eden 清空
   [Survivor: 64.0M->64.0M(64.0M)]            ← Survivor 使用
   [Heap: 3.2G(4.0G)->2.7G(4.0G)]             ← 堆总使用变化
   [Times: user=0.02 sys=0.00, real=0.01 secs] ← 实际耗时
```

---

## 六、GC 选型建议

| 场景 | 推荐 GC | 理由 |
|---|---|---|
| 小堆（< 4G）、单线程 | Serial | 简单高效，无线程切换 |
| 吞吐量优先、无特殊延迟要求 | Parallel | 多线程并行 STW，吞吐高 |
| 低延迟、堆 4-8G | CMS（JDK 14前） | 并发标记，停顿较少 |
| **大堆（≥ 6G）、可预测延迟** | **G1**（JDK 9+ 默认） | Region 化 + Mixed GC |
| 超大堆（> 16G）、极低延迟 | ZGC / Shenandoah | 亚毫秒级停顿 |

---

## 七、终极背诵总结

1. **CMS 四阶段**：初始标记（STW短）→ 并发标记（最长，不STW）→ 重新标记（STW中）→ 并发清除（不STW，留碎片）
2. **CMS 三大缺点**：CPU 敏感 + 浮动垃圾 + 碎片化
3. **Concurrent Mode Failure**：老年代空间不够 → 退化 Serial Old → STW 暴增
4. **G1 Region**：堆分 2048 个 Region，Eden/Survivor/Old/Humongous 四种
5. **G1 Mixed GC**：回收全年轻代 + 部分垃圾最多的老年代 Region
6. **G1 优势**：可预测停顿 + 无碎片 + Region 灵活 + 增量回收
7. **选型**：大堆 G1，小堆 Parallel，超大堆低延迟 ZGC
8. **Xms == Xmx**：避免堆动态扩缩的开销
