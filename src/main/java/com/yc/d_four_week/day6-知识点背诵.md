# Day6 OOM 类型、内存泄漏、Full GC 排查 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、OOM 常见类型与原因

### 1.1 四大 OOM 类型

| OOM 类型 | 错误信息 | 典型原因 |
|---|---|---|
| **堆溢出** | `java.lang.OutOfMemoryError: Java heap space` | 对象太多/太大，堆不够用（最常见） |
| **元空间溢出** | `OOM: Metaspace` | 动态生成大量类（CGLIB/JSP/动态代理），元空间不够 |
| **栈溢出** | `StackOverflowError` | 递归太深、方法调用层次过多 |
| **无法创建线程** | `OOM: unable to create new native thread` | 线程数超 OS 限制（每个线程占独立栈内存） |
| **直接内存溢出** | `OOM: Direct buffer memory` | NIO DirectByteBuffer 使用过量堆外内存 |

### 1.2 各类型深入

#### 堆溢出
```java
// 不断往 List 加对象
List<byte[]> list = new ArrayList<>();
while (true)
    list.add(new byte[1024 * 1024]);  // → Java heap space
```
**排查**：`jmap -dump → MAT 分析 → 找最大对象 → 追溯引用链`

#### 元空间溢出
```java
// CGLIB 无限生成代理类 → 类元数据过多
while (true) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(SomeClass.class);
    enhancer.create();  // 每次生成新 class
}
```
**解决**：`-XX:MaxMetaspaceSize=256m` 设上限，排查是否不必要地重复创建代理类

#### 栈溢出
```java
public void recursive() {
    recursive();  // 无限递归 → StackOverflowError
}
```
**区别**：
- `StackOverflowError`：递归太深，栈深度超限
- `OOM: unable to create new native thread`：线程太多，每个线程都占栈内存，OS 内存不够分配新栈

---

## 二、内存泄漏常见原因

### 2.1 ThreadLocal 未 remove（最经典）

```java
// 线程池 + ThreadLocal = 内存泄漏
ThreadLocal<User> local = new ThreadLocal<>();
local.set(new User("张三"));
// 用完没调 local.remove()！
// 线程池线程复用 → Thread.threadLocals 一直持有 → User 无法回收
```

**泄漏链路**：Thread（线程池常驻）→ ThreadLocalMap → Entry（key 被弱引用回收了，value 还在）→ User 泄漏。

### 2.2 静态集合不断添加

```java
static List<Object> list = new ArrayList<>();  // 静态变量是 GC Root
// 不断往 list 加数据，从不清理 → list 引用的对象永远不回收
```

### 2.3 连接/流未关闭

```java
Connection conn = dataSource.getConnection();
// ... 用完没 close()
// → 连接池耗尽 → 新请求获取不到连接 → OOM 或卡死
```

### 2.4 Listener 未注销

```java
// 注册了监听器，但用完后忘记 removeListener
// → Observer 持有 Subject 引用 → Subject 不能被 GC
```

### 2.5 内部类持有外部引用

```java
// 非静态内部类隐式持有外部类的引用
class Outer {
    class Inner { }  // Inner 持有 Outer.this
}
// Outer 应该被回收时，如果 Inner 被其他地方引用 → Outer 泄漏
```

---

## 三、Full GC 排查 SOP ⭐⭐

### 3.1 完整排查流程

```
线上告警：GC 频繁、CPU 飙高、响应变慢
        │
        ▼
第 1 步：确认现状
  ┌─ jps -l                      查看 Java 进程 PID
  ├─ jstat -gcutil <pid> 1000    每秒输出一次 GC 数据
  ├─ top -Hp <pid>               查看线程 CPU 占比
  └─ jinfo <pid>                 查看 JVM 参数
        │
        ▼
第 2 步：定位原因
  Full GC 频繁 可能原因：
  ┌─ 老年代增长快 → 晋升速率高 → 调大年轻代 / 优化代码减少对象
  ├─ 大对象直接进老年代 → 检查是否有大数组/大缓存
  ├─ 元空间不足 → 检查动态代理/反射/CGLIB
  ├─ 内存泄漏 → dump 分析（下面第 3 步）
  └─ System.gc() → 检查代码中是否显式调用
        │
        ▼
第 3 步：Dump 分析（如果是内存泄漏）
  jmap -dump:format=b,file=heap.hprof <pid>
  → MAT（Memory Analyzer Tool）打开 dump
    → Histogram：查看对象实例数和占用大小
    → Dominator Tree：找到占用最大的对象
    → Leak Suspects：自动分析泄漏嫌疑
    → GC Roots Path：追溯引用链找到泄漏根因
        │
        ▼
第 4 步：线程排查（如果是 CPU 飙高）
  jstack <pid> > thread.txt
  → 找 RUNNABLE 状态的线程 → 看调用栈 → 定位代码
  → 找 BLOCKED / 死锁 → jstack 会打印死锁信息
        │
        ▼
第 5 步：止血 + 根治
  止血：
    ├─ 重启（临时恢复）
    ├─ 扩堆（-Xmx 调大）
    └─ 限流（减少涌入流量）
  根治：
    ├─ 修复内存泄漏代码
    ├─ 优化大对象创建逻辑
    └─ GC 参数调优
```

### 3.2 必备命令速查

| 命令 | 作用 | 示例 |
|---|---|---|
| `jps -l` | 查看 Java 进程 | — |
| `jstat -gcutil <pid> 1000` | 每秒看 GC 各区域占比 | 关注 FGC/FGCT 列 |
| `jmap -histo <pid>` | 对象直方图（不 dump，快速看） | 找大量重复创建的对象 |
| `jmap -dump:format=b,file=xxx.hprof <pid>` | 生成堆 dump | 配合 MAT 分析 |
| `jstack <pid>` | 线程栈 | 找死锁、CPU 高的线程 |
| `jinfo <pid>` | JVM 运行参数 | 确认参数是否生效 |
| `jcmd <pid> GC.heap_dump xxx.hprof` | 替代 jmap（推荐） | JDK 9+ |

---

## 四、Full GC 触发场景

| 触发场景 | 说明 |
|---|---|
| **老年代空间不足** | 最频繁的原因，Young GC 晋升对象老年代放不下 |
| **元空间不足** | Metaspace 满了 → Full GC 回收类元数据 + 无效类 |
| **System.gc()** | 建议 JVM 做 Full GC（`-XX:+DisableExplicitGC` 可禁用） |
| **CMS Concurrent Mode Failure** | CMS 并发清理时老年代不够放晋升对象 |
| **晋升失败（Promotion Failed）** | Minor GC 时老年代没有足够连续空间 |
| **分配担保失败** | 老年代剩余空间 < 年轻代全部对象平均大小 |

---

## 五、常见排查案例分析

### 5.1 频繁 Young GC

**现象**：Young GC 几秒一次，每次时间短。

**原因**：年轻代太小，或代码中大量创建短生命周期对象。

**解决**：`-Xmn` 调大年轻代，或优化代码减少对象创建。

### 5.2 频繁 Full GC

**现象**：每次都触发 Full GC，且回收后老年代占用率仍然很高。

**原因**：
- 堆太小，业务对象太多 → 调大 -Xmx
- 内存泄漏 → dump 分析
- 大对象直入老年代 → 检查 PretenureSizeThreshold / 代码

### 5.3 CPU 100% + Full GC

**现象**：CPU 飙满，GC 日志显示频繁 Full GC。

**原因**：
- 内存泄漏 → 老年代撑满 → 每次 GC 都回收不了多少 → GC 线程空转
- CMS 并发模式失败 → 退化 Serial Old → STW 阻塞所有业务线程

**解决**：dump → MAT → 找到泄漏根因。

---

## 六、JVM 参数排查模板

```bash
# 标准排查参数配置（应在所有生产环境默认加上）
-Xms4g -Xmx4g                          # 堆 4G（初始=最大，避免扩缩）
-Xmn2g                                  # 年轻代 2G
-XX:+UseG1GC                            # 使用 G1
-XX:MaxGCPauseMillis=200                # 期望最大停顿 200ms
-XX:+HeapDumpOnOutOfMemoryError         # OOM 自动 dump
-XX:HeapDumpPath=/data/logs/heap.hprof  # dump 路径
-XX:+PrintGCDetails                     # GC 详细日志（JDK8）
-Xlog:gc*:/data/logs/gc.log:time,level  # GC 日志（JDK9+）
-XX:MaxMetaspaceSize=256m               # 元空间上限
-XX:+PrintCommandLineFlags              # 打印 JVM 实际生效参数
```

---

## 七、终极背诵总结

1. **4 种 OOM**：堆溢出（对象太多）/ 元空间溢出（类太多）/ 栈溢出（递归太深）/ 直接内存溢出（堆外）
2. **内存泄漏 5 大元凶**：ThreadLocal / 静态集合 / 连接未关 / Listener 未注销 / 内部类引用
3. **Full GC 排查 SOP**：jstat 看频率 → jmap dump → MAT 找泄漏对象 → GC Roots 追溯 → 修复代码
4. **Full GC 6 大触发**：老年代不够 / 元空间不足 / System.gc / CMS 失败 / 晋升失败 / 担保失败
5. **必备命令**：jps / jstat / jmap / jstack / jinfo / jcmd
6. **生产标配参数**：Xms=Xmx + OOM dump + GC 日志 + 元空间上限
