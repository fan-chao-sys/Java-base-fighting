# day5-知识点背诵.md

# Day5 Java泛型底层核心知识点背诵笔记 ⭐⭐

---

## 一、泛型的核心定义与作用

### 1. 什么是泛型
泛型（Generics）是Java 5引入的一种**参数化类型**机制，允许在类、接口、方法中定义类型参数（如 `<T>`），实现代码的类型复用与编译期类型检查。

### 2. 泛型的核心作用（必背）
- **编译期类型安全检查**：在编译阶段检查存入集合的类型，避免运行时`ClassCastException`。
- **消除强制类型转换**：从泛型集合中取出元素时，无需手动强转，编译器自动完成。
- **提升代码复用性**：一套代码可适配多种数据类型，无需为不同类型重复实现。

---

## 二、类型擦除（泛型的底层实现）

### 1. 类型擦除的定义
Java泛型是**编译期语法糖**，泛型信息仅存在于编译阶段，编译后的字节码中所有泛型信息都会被擦除，运行时不存在泛型类型。

### 2. 擦除规则（核心底层）
- **无界类型擦除**：`<T>` 擦除后变为 `Object`。
- **有界类型擦除**：`<T extends Number>` 擦除后变为上界类型 `Number`。
- **方法泛型擦除**：方法上的类型参数同样会被擦除，替换为对应的上界类型。

#### 示例：擦除前后对比
```java
// 编译前
public class Box<T> {
    private T value;
    public T getValue() { return value; }
    public void setValue(T value) { this.value = value; }
}

// 编译后（擦除后）
public class Box {
    private Object value;
    public Object getValue() { return value; }
    public void setValue(Object value) { this.value = value; }
}
