# Day6 Java反射、注解、动态代理底层核心知识点背诵笔记

---

## 一、反射（Reflection）底层原理与核心

### 1. 反射的核心定义
Java反射机制，是指**程序在运行时，可以获取类的完整信息，并直接操作类的对象、方法、字段**的能力。
- 打破了编译期的类型限制，让程序具备动态扩展能力。
- 核心入口：`java.lang.Class` 对象。

### 2. 反射的底层本质
- 每个类被JVM加载后，都会在方法区生成一个对应的`Class`对象，存储该类的所有元数据（字段、方法、构造器等）。
- 反射就是通过`Class`对象，直接访问和操作这些元数据，绕过编译期权限检查。

### 3. 获取 `Class` 对象的三种方式（必背）
```java
// 方式1：类名.class（编译期已知类）
Class<?> clazz = User.class;

// 方式2：对象.getClass()（运行时已有对象）
User user = new User();
Class<?> clazz = user.getClass();

// 方式3：Class.forName("全限定类名")（运行时动态加载类）
Class<?> clazz = Class.forName("com.example.User");
