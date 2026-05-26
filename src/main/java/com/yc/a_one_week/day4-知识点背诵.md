# day4-知识点背诵.md

# Day4 Java异常体系底层核心知识点背诵笔记

---

## 一、Java异常体系整体架构

### 1. 异常体系根类：`Throwable`
- 所有Java异常/错误的顶层父类，位于`java.lang`包下。
- 分为两大分支：`Error` 和 `Exception`。

---

### 2. `Error` 类（JVM级错误）
- **定义**：表示JVM无法处理的严重错误，如系统级错误、资源耗尽。
- **核心特性**：
  - 属于非受检异常，编译器不强制处理。
  - 程序无法恢复，无需在代码中捕获处理。
- **常见示例**：`OutOfMemoryError`、`StackOverflowError`、`NoClassDefFoundError`。

---

### 3. `Exception` 类（可处理的异常）
- **定义**：程序运行时可能出现的可恢复异常，分为两类：
  1.  **受检异常（Checked Exception）**
  2.  **非受检异常（Unchecked Exception）**

---

## 二、受检异常 vs 非受检异常

### 1. 受检异常（Checked Exception）
- **定义**：继承自`Exception`但不继承`RuntimeException`的异常，编译器强制处理。
- **核心特性**：
  - 编译期检查，必须通过`try-catch`捕获或`throws`声明抛出。
  - 用于可预见的外部错误（如IO、网络、文件操作）。
- **常见示例**：`IOException`、`SQLException`、`ClassNotFoundException`。

---

### 2. 非受检异常（Unchecked Exception）
- **定义**：继承自`RuntimeException`及其子类的异常，编译器不强制处理。
- **核心特性**：
  - 运行期才会出现，通常由代码逻辑错误导致。
  - 不强制捕获/声明，建议通过优化代码避免。
- **常见示例**：`NullPointerException`、`ArrayIndexOutOfBoundsException`、`ArithmeticException`、`ClassCastException`。

---

## 三、`try-catch-finally` 底层执行机制

### 1. 核心执行顺序（必背）
1.  执行`try`块代码。
2.  若`try`中无异常：执行`finally`块，程序继续。
3.  若`try`中出现异常：匹配对应`catch`块执行，再执行`finally`块。
4.  特殊情况：`try`或`catch`中有`return`语句，仍会先执行`finally`再返回。

---

### 2. `finally` 关键字特性
- **核心作用**：用于释放资源（如IO流、数据库连接），保证代码一定会执行。
- **底层原理**：JVM通过异常表（Exception Table）实现，`finally`块的字节码会被复制到所有正常和异常退出路径中。
- **特殊情况：finally 不执行的场景**： ⭐
  1.  调用`System.exit(0)`直接终止JVM。
  2.  程序所在线程死亡。
  3.  硬件故障/系统关机。

---

### 3. `finally` 中 `return` 的坑 ⭐⭐⭐
- 若`finally`块中存在`return`语句，会**覆盖`try`/`catch`中的`return`值**。
- 底层原理：`try`中`return`时，会先将返回值压入操作数栈；但`finally`中的`return`会修改操作数栈顶的值，导致最终返回`finally`中的值。
- 更严重的问题：如果 finally 里写了 return，try 里的异常会被吞掉，外部无法捕获，导致 bug 排查困难。
```java
public int test() {
    int a = 10;
    try {
        return a; // 先将10压栈
    } finally {
        a = 20;
        return a; // 修改栈顶值为20，最终返回20
    }
}
