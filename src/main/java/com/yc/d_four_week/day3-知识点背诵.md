# Day3 类加载过程、双亲委派、类加载器 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、类加载 5 阶段（完整生命周期）

```
加载 → 验证 → 准备 → 解析 → 初始化 → 使用 → 卸载
                                        ↑
                                   （这7个阶段构成完整的类生命周期）
```

### 1.1 五个阶段逐一吃透

#### ① 加载（Loading）

**任务**：
1. 通过**全限定名**获取类的二进制字节流（class 文件 / jar / 网络 / 动态生成）
2. 将字节流转为**方法区的运行时数据结构**
3. 在堆中生成 `java.lang.Class` 对象，作为方法区这个类的**访问入口**

**加载来源**：
- 本地 class 文件（最常见）
- jar/zip 包中的 class
- 网络流（Applet）
- 运行时计算生成（动态代理 `$Proxy0`、CGLIB）

#### ② 验证（Verification）

| 验证阶段 | 检查内容 |
|---|---|
| **文件格式验证** | 是否以 0xCAFEBABE 开头、版本号是否在 JVM 支持范围内 |
| **元数据验证** | 是否有父类（除 Object）、是否继承了 final 类、抽象方法是否实现 |
| **字节码验证** | 指令跳转是否越界、类型转换是否合法 |
| **符号引用验证** | 符号引用指向的类/方法/字段是否存在、访问权限是否足够 |

> 验证是**保证 JVM 安全**的防线，拒绝恶意字节码。

#### ③ 准备（Prepare）

- 为**静态变量**分配内存并赋**零值**
- **例外**：`static final` 修饰的常量 → 直接赋代码中的值（编译期已确定）

```java
static int a = 123;       // 准备阶段：a = 0（零值）
                           // 初始化阶段：a = 123

static final int b = 456; // 准备阶段：b = 456（直接赋值，编译期常量）
```

#### ④ 解析（Resolve）

将常量池中的**符号引用**替换为**直接引用**（内存地址/偏移量）。

| 符号引用 | 直接引用 |
|---|---|
| `#3 = Methodref #15.#16` | 方法在内存中的实际入口地址 |
| 编译期不知道具体地址 | 运行期可直接定位 |

**解析目标**：类/接口 → 字段 → 方法 → 接口方法（4 类符号引用）

#### ⑤ 初始化（Initialization）

执行类构造器 `<clinit>()` 方法：
- **static 变量赋值** + **static 代码块**，按代码顺序合并执行
- 父类的 `<clinit>` 先于子类执行
- 多线程同时初始化一个类 → JVM 加锁保证**只初始化一次**

**触发初始化的 6 种情况（主动引用）**：
1. new / getstatic / putstatic / invokestatic 指令
2. 反射调用
3. 初始化子类 → 先触发父类初始化
4. main 方法所在类
5. MethodHandle 调用
6. JDK 8 接口的 default 方法

---

## 二、双亲委派模型（Parents Delegation Model）⭐⭐⭐

### 2.1 三层结构

```
┌─────────────────────────────────────┐
│  Bootstrap ClassLoader （启动类加载器） │  ← C++ 实现，加载 <JAVA_HOME>/lib 核心类
│  加载：rt.jar / java.lang.*          │     （如 String、Object、ArrayList）
└──────────────┬──────────────────────┘
               │ 父加载器
               ▼
┌─────────────────────────────────────┐
│  Platform ClassLoader （平台类加载器） │  ← JDK 9+（原 ExtClassLoader）
│  加载：<JAVA_HOME>/lib/ext 扩展类     │
└──────────────┬──────────────────────┘
               │ 父加载器
               ▼
┌─────────────────────────────────────┐
│  Application ClassLoader（应用类加载器）│  ← 加载 classpath 下的类
│  加载：自定义类和第三方 jar            │     （用户自己写的类）
└─────────────────────────────────────┘
```

### 2.2 委派机制执行流程

```
loadClass(className) 调用链：

AppClassLoader.loadClass("com.example.User")
  │
  ├─→ 检查是否已经加载过？ → 已加载 → 直接返回
  │        │ 未加载
  │        ▼
  └─→ 委托给父加载器 PlatformClassLoader.loadClass()
           │
           └─→ 委托给父加载器 BootstrapClassLoader.loadClass()
                  │
                  ├─→ 能加载吗？（在 rt.jar 里？）→ 能 → 加载返回
                  │        │
                  │        └→ 不能 → 返回给 Platform
                  │
                  └─→ Platform 自己能加载吗？ → 能 → 加载返回
                         │
                         └→ 不能 → 返回给 AppClassLoader
                                      │
                                      └→ 自己加载（从 classpath 找）
```

**口诀**：**自底向上查缓存，自顶向下试加载**

### 2.3 双亲委派的好处

| 好处 | 说明 |
|---|---|
| **防止核心类被篡改** | 你写的 `java.lang.String` 不会生效 → Bootstrap 先加载到，你的永远不会被加载 |
| **避免重复加载** | 同一个类只被加载一次，全限定名 + 类加载器 确定唯一性 |
| **安全沙箱** | 核心 API 的类由 JVM 信任的加载器加载，不可被替换 |

### 2.4 打破双亲委派的场景

| 场景 | 为什么打破 | 怎么做的 |
|---|---|---|
| **JDBC（SPI）** | 接口 `java.sql.Driver` 在 rt.jar（Bootstrap 加载），实现类在用户 classpath（AppClassLoader 加载）→ Bootstrap 需要"向下"调用 App 加载的类 | **线程上下文类加载器** `Thread.setContextClassLoader()` |
| **Tomcat** | 多个 Web 应用需要类隔离（各自不同版本的 Spring jar） | `WebAppClassLoader` 打破双亲委派，先自己加载，找不到再委托给父 |
| **OSGi** | 模块化，每个 Bundle 有自己的类加载器 | 网状委派模型，不遵循双亲委派 |
| **热部署** | 同一个类重新加载（不重启 JVM） | 自定义 ClassLoader，每次加载新的 class 文件 |

---

## 三、类加载器源码剖析

### 3.1 loadClass 核心逻辑

```java
protected Class<?> loadClass(String name, boolean resolve) {
    synchronized (getClassLoadingLock(name)) {
        // ① 先检查是否已加载
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                // ② 委托父加载器
                if (parent != null)
                    c = parent.loadClass(name, false);
                else
                    c = findBootstrapClassOrNull(name);  // Bootstrap
            } catch (ClassNotFoundException e) { }
            // ③ 父加载不了 → 自己加载
            if (c == null)
                c = findClass(name);  // AppClassLoader 在这里从 classpath 读
        }
        return c;
    }
}
```

**打破双亲委派的方法**：重写 `loadClass()` 改变委派顺序，或重写 `findClass()` 改变加载方式。

---

## 四、类的唯一性

**同一个类在 JVM 中的唯一性 = 全限定名 + 类加载器**

- 同一个 class 文件，被**不同的类加载器**加载 → 是两个不同的 Class 对象
- 这就是 Tomcat 实现多 Web 应用隔离的原理：每个应用一个独立的 WebAppClassLoader

---

## 五、高频易错点

### 1. 加载 vs 初始化
- **加载** ≠ **初始化**：`Class.forName("xxx")` 默认初始化，`ClassLoader.loadClass("xxx")` 只加载不初始化
- 很多框架用 `loadClass` 延迟初始化，再通过其他手段触发

### 2. static final 的准备阶段
- `static final int X = 1;` → **准备阶段就赋值 1**
- `static final Object X = new Object();` → **准备阶段赋 null**（编译器无法确定值）

### 3. `<clinit>` vs `<init>`
- `<clinit>`：类构造器，执行 static 变量赋值和 static 代码块
- `<init>`：实例构造器，执行实例变量赋值和非 static 代码块 + 构造方法

---

## 六、终极背诵总结

1. **类加载 5 阶段**：加载（读字节流）→ 验证（安全检查）→ 准备（static 变量赋零值）→ 解析（符号→直接引用）→ 初始化（static 赋值+代码块）
2. **双亲委派**：AppClassLoader → PlatformClassLoader → BootstrapClassLoader，自底向上查缓存，自顶向下试加载
3. **双亲委派好处**：防止核心类被篡改 + 避免重复加载 + 安全隔离
4. **打破场景**：SPI（线程上下文类加载器）/ Tomcat（WebAppClassLoader）/ OSGi / 热部署
5. **唯一性**：全限定名 + 类加载器 → 两个加载器加载同一个 class = 两个不同的 Class
6. **准备阶段特殊**：static final 常量在准备阶段直接赋值，普通 static 变量只赋零值
