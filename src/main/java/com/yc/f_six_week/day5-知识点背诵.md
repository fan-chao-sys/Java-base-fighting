# Day5 循环依赖、三级缓存、Spring MVC 请求流程 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、循环依赖 —— 什么是，怎么解决

### 1.1 循环依赖场景

```
A 依赖 B, B 依赖 A
┌───┐      ┌───┐
│ A │ ←──→ │ B │
└───┘      └───┘
```

```java
@Component
public class A {
    @Autowired private B b;  // A 需要 B
}

@Component
public class B {
    @Autowired private A a;  // B 需要 A
}
```

**Spring 能解决**：singleton + setter/字段注入的循环依赖。

**Spring 不能解决**：**构造器注入**的循环依赖（因为在实例化阶段就需要依赖，还没有半成品可用）。

---

## 二、三级缓存 —— Spring 解决循环依赖的核心 ⭐⭐⭐

### 2.1 三级缓存结构

| 缓存 | 名称 | 存储内容 | 说明 |
|---|---|---|---|
| **一级** | `singletonObjects` | **完全初始化好**的单例 Bean | 对外暴露的就是这个 |
| **二级** | `earlySingletonObjects` | **提前暴露的半成品**（已实例化，未填属性） | A 给 B 的就是这个 |
| **三级** | `singletonFactories` | **ObjectFactory**（可生成早期引用或代理） | 解决 AOP 代理问题 |

### 2.2 解决流程（A ↔ B 循环依赖）

```
① getBean(A)：A 不存在 → 实例化 A（反射 new A，属性全 null）
              → A 的 ObjectFactory 放入三级缓存

② populateBean(A)：A 需要 B → getBean(B)

③ B 不存在 → 实例化 B → B 的 ObjectFactory 放入三级缓存

④ populateBean(B)：B 需要 A → getBean(A)
              → 从三级缓存拿到 A 的 ObjectFactory → 调用 getObject()
              → 得到 A 的半成品引用 → 放入二级缓存 → 返回给 B

⑤ B 拿到 A 的半成品引用 → B 完成属性填充 → B 初始化完成
              → B 放入一级缓存

⑥ 回到 A：B 已经完整 → A 拿到 B → A 填充属性完成 → A 初始化完成
              → A 放入一级缓存
```

**口诀**：A 实例化 → 三级缓存放工厂 → A 发现缺 B → B 实例化 → B 找 A → 三级缓存给 B → B 完成 → A 完成

### 2.3 为什么需要三级缓存？两级不行吗？⭐

**核心答案**：**为了处理 AOP 代理。**

- 二级缓存只存一个"半成品对象" → 如果 A 需要被 AOP 代理，B 拿到的是原始 A 还是代理 A？
- 三级缓存存的是 `ObjectFactory` → 可以在后期根据"A 是否需要 AOP"动态决定返回原始对象还是代理对象
- 本质是**解耦了"获取半成品"和"是否要代理"两个时机**

### 2.4 构造器循环依赖为什么不行？

构造器注入发生在**实例化阶段**，此时对象还没创建出来，无法放入三级缓存的 ObjectFactory → B 找不到 A 的半成品 → 死循环 → `BeanCurrentlyInCreationException`。

---

## 三、Spring MVC 请求流程

### 3.1 完整执行链路

```
HTTP 请求 → DispatcherServlet
    │
    ├─①─→ HandlerMapping —— 根据 URL 找到对应的 Handler（Controller 方法）
    │     （RequestMappingHandlerMapping 找 @RequestMapping 匹配的方法）
    │
    ├─②─→ HandlerAdapter —— 调用 Handler
    │     （RequestMappingHandlerAdapter 处理参数绑定、类型转换、验证）
    │
    ├─③─→ Handler（Controller）—— 执行业务逻辑，返回 ModelAndView / @ResponseBody
    │     （你的业务代码）
    │
    ├─④─→ ViewResolver —— 解析视图名称 → 找到具体 View
    │     （InternalResourceViewResolver → JSP / Thymeleaf）
    │
    └─⑤─→ View 渲染 —— 把 Model 数据填充到视图 → 返回 HTML
          或 @ResponseBody → HttpMessageConverter → 直接返回 JSON
```

### 3.2 核心组件速查

| 组件 | 作用 |
|---|---|
| **DispatcherServlet** | 前端控制器，所有请求的统一入口 |
| **HandlerMapping** | URL → Handler 映射（哪个 Controller 方法处理） |
| **HandlerAdapter** | 执行 Handler（处理参数绑定、类型转换） |
| **Handler（Controller）** | 开发者写的业务逻辑 |
| **ViewResolver** | 逻辑视图名 → 物理视图（JSP/Thymeleaf） |
| **HttpMessageConverter** | 将 Java 对象转为 JSON/XML 返回 |

---

## 四、终极背诵总结

1. **三级缓存**：一级（成品）、二级（半成品）、三级（ObjectFactory）
2. **三级作用**：AOP 代理问题 — 三级存工厂，按需决定返回原始或代理对象
3. **解决流程**：A→三级存工厂→缺 B→B 实例化→B 缺 A→从三级拿 A 半成品→B 完成→A 完成
4. **构造器不行**：实例化阶段就需要依赖 → 还没放三级缓存 → 无法获取
5. **Spring MVC 五步**：HandlerMapping 定位 → HandlerAdapter 执行 → Controller 处理 → ViewResolver 解析 → View 渲染
