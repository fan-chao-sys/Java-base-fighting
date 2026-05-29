# Day7 周测知识点总结 —— Spring、Spring Boot、事务与设计模式

> 覆盖 Day1~Day6 全部核心知识点，对应"学习任务清单" Day 7 整周复盘要求。

---

## 一、10 道自测题（口述标准答案）

### Q1: IOC 和 AOP 分别是什么？解决了什么问题？

**答：**

| | IOC（控制反转） | AOP（面向切面编程） |
|---|---|---|
| **定义** | 把对象的创建和管理交给 Spring 容器，不在代码中 `new` | 把横切关注点（日志/事务/鉴权）从业务代码中分离 |
| **解决问题** | 对象之间的**耦合**（解耦） | 代码的**重复**（复用横切逻辑） |
| **实现** | DI（依赖注入）：容器自动把依赖"注入" | 动态代理（JDK/CGLIB）：拦截方法织入通知 |
| **关系** | IOC 是 AOP 的基础，AOP 是 IOC 的扩展应用 | — |

**一句话**：IOC 管对象怎么来，AOP 管方法怎么增强。

---

### Q2: Bean 生命周期包含哪些关键步骤？AOP 在哪一步介入？

**答（完整链路由 W6D2_Actual 验证）：**

```
实例化（反射 new） → 属性填充（@Autowired 注入）
  → BeanNameAware.setBeanName()
  → BeanPostProcessor.before（@Value 解析）
  → @PostConstruct
  → InitializingBean.afterPropertiesSet()
  → init-method
  → ★ BeanPostProcessor.after（AOP 代理在此生成！）★
  → Bean 就绪，放入 singletonObjects（一级缓存）
  → @PreDestroy → DisposableBean.destroy() → destroy-method
```

**AOP 拦截点**：`BeanPostProcessor.postProcessAfterInitialization` → 检查是否有切面匹配 → 有则创建代理对象替代原 Bean。

---

### Q3: BeanFactory 和 ApplicationContext 的区别？

**答：**

| | BeanFactory | ApplicationContext |
|---|---|---|
| **层级** | 底层 IoC 容器接口 | 继承 BeanFactory，功能更丰富 |
| **加载时机** | **懒加载**：`getBean()` 时才创建 | **启动时**全量初始化单例 Bean |
| **功能** | Bean 的注册和获取 | + 事件发布 + 国际化 + 资源加载 + AOP + 环境抽象 |

**W6D1_Actual 注释**：绘制了完整容器继承链 `BeanFactory → ... → DefaultListableBeanFactory → ApplicationContext → WebApplicationContext`。

---

### Q4: Spring 三级缓存分别存什么？为什么需要三级？

**答：**

| 缓存 | 名称 | 存储内容 |
|---|---|---|
| **一级** `singletonObjects` | 成品库 | 完全初始化好的单例 Bean |
| **二级** `earlySingletonObjects` | 半成品库 | 提前暴露的 Bean（已实例化，未填充属性） |
| **三级** `singletonFactories` | 工厂库 | `ObjectFactory`（可生成早期引用或 AOP 代理） |

**为什么需要三级？** 为了处理 **AOP 代理**问题。如果 A 需要被代理，B 拿到的是原始 A 还是代理 A？三级缓存存 `ObjectFactory`，可以在后期根据"A 是否需要 AOP"动态决定返回原始对象还是代理对象。

**解决流程（A↔B 循环依赖）**：
```
A 实例化 → 三级缓存放 A 的 ObjectFactory → A 缺 B
→ B 实例化 → B 缺 A → 三级缓存给 B（A 的半成品引用）
→ B 完成 → A 拿到完整 B → A 完成
```

---

### Q5: 构造器注入的循环依赖为什么解决不了？

**答：** 构造器注入发生在**实例化阶段**，此时对象还没创建出来（构造函数还没执行完），无法放入三级缓存的 ObjectFactory → B 调用 A 的构造函数时，A 还不存在 → `BeanCurrentlyInCreationException`。

**setter/字段注入**：先实例化（反射 no-arg 构造）→ 三级缓存放 ObjectFactory → 再填属性 → B 填属性时能从三级缓存拿到 A 的半成品。

---

### Q6: @Transactional 失效的常见场景有哪些？（至少说 5 种）

**答（W6D4_Actual + TransactionDemoService 验证）：**

| 序号 | 场景 | 失效原因 | 验证代码 |
|---|---|---|---|
| ① | 注解在 **private** 方法上 | CGLIB/JDK 代理只能拦截 public | `@Transactional private void insertUser()` |
| ② | **同类内部调用** | `this.method()` 不走代理 | `public void callInsertUser() { this.insertUser(); }` |
| ③ | 异常被 **catch 吞了** | AOP 只看未捕获的异常 | `try { ... throw e; } catch (Exception e) { /* 吞了 */ }` |
| ④ | **rollbackFor 未配** | 默认只回滚 `RuntimeException`，受检异常不回滚 | `@Transactional` 不加 `rollbackFor` |
| ⑤ | **数据库引擎不支持** | MyISAM 无事务 | `ENGINE=MyISAM` |
| ⑥ | 多线程中调用 | 子线程不受 Spring 事务管理 | `new Thread(() -> service.method()).start()` |
| ⑦ | 类未纳入 Spring 容器 | 不是 Bean，不存在代理 | `new Xxx()` 而非 `@Autowired` |

**解决同类内部调用**：① 抽到另一个 Service ② `AopContext.currentProxy()` ③ `@Autowired private Self self;`

---

### Q7: REQUIRED 和 REQUIRES_NEW 传播行为的区别？

**答：**

| | REQUIRED（默认） | REQUIRES_NEW |
|---|---|---|
| **当前有事务** | **加入**已有事务 | **新建**事务，挂起当前 |
| **当前无事务** | 新建事务 | 新建事务 |
| **回滚影响** | 一个回滚全都回滚（同生共死） | 各自独立回滚 |
| **典型场景** | 订单扣库存（整体成功/失败） | 记录操作日志（主事务失败，日志也得留） |

```
A.required() → B.required()
B 抛异常 → A+B 都回滚

A.required() → B.requiresNew()
B 抛异常 → B 回滚，A 不受影响
```

---

### Q8: Spring MVC 一个请求的完整执行过程？

**答：**

```
HTTP 请求
  │
  ▼
DispatcherServlet（前端控制器，统一入口）
  │
  ├─①─→ HandlerMapping —— 根据 URL 找对应的 Controller 方法
  │     （@RequestMapping 路径匹配）
  │
  ├─②─→ HandlerAdapter —— 执行 Handler（参数绑定/类型转换）
  │
  ├─③─→ Handler（Controller）—— 你的业务逻辑
  │     返回 ModelAndView 或 @ResponseBody（JSON）
  │
  ├─④─→ ViewResolver —— 视图名 → 具体 View（JSP/Thymeleaf）
  │     （@RestController 跳过这一步，直接 JSON）
  │
  └─⑤─→ View 渲染 / HttpMessageConverter —— 返回 HTML/JSON 给客户端
```

---

### Q9: Spring Boot 自动装配从 @SpringBootApplication 到加载 Bean 的步骤？

**答：**

```
@SpringBootApplication
  └── @EnableAutoConfiguration
        └── @Import(AutoConfigurationImportSelector.class)
              └── 读取 META-INF/spring/
                    org.springframework.boot.autoconfigure.AutoConfiguration.imports
                    （Spring Boot 3+，旧版 spring.factories）
                    └── 按 @Conditional 过滤
                          ├── @ConditionalOnClass（classpath 有才加载）
                          ├── @ConditionalOnMissingBean（用户没定义才创建）
                          └── @ConditionalOnProperty（配置匹配才生效）
                              └── 加载配置类 → 创建 Bean
```

**本周验证（my_log_starter）**：自定义 `LogAutoConfiguration` + `@ConditionalOnMissingBean` + `AutoConfiguration.imports` 文件 → 实现了完整的自定义 Starter。

---

### Q10: Spring 中用到了哪些设计模式？各举一例

**答：**

| 设计模式 | Spring 中的体现 | 一句话 |
|---|---|---|
| **单例模式** | Bean 默认作用域 `singleton` | 每个 Bean 只有一个实例 |
| **工厂模式** | `BeanFactory` / `ApplicationContext` | 把对象创建和使用分离 |
| **代理模式** | **AOP** — JDK 动态代理 / CGLIB | 不改原方法，增强功能 |
| **模板方法** | `JdbcTemplate` / `RedisTemplate` / `RestTemplate` | 固定流程，子步骤可变 |
| **观察者模式** | `ApplicationEvent` + `@EventListener` | 事件驱动松耦合 |
| **策略模式** | `DispatcherServlet` 的 `HandlerMapping` | 按 URL 选择不同 Controller |
| **责任链模式** | Filter / Interceptor 链 | 请求逐层过滤 |

---

## 二、Spring 全家桶高频题

### 2.1 IOC 容器继承链

```
BeanFactory（接口，顶层）
  └── ListableBeanFactory
       └── ConfigurableBeanFactory
            └── AbstractBeanFactory
                 └── DefaultListableBeanFactory（基础实现）
                      └── ApplicationContext（子接口，超集）
                           ├── AnnotationConfigApplicationContext（注解）
                           └── WebApplicationContext（Web 环境）
                                └── ServletWebServerApplicationContext（Spring Boot）
```

### 2.2 AOP 五通知执行顺序

```
正常：① @Around 前 → ② @Before → [目标方法] → ③ @AfterReturning → ④ @After → ⑤ @Around 后

异常：① @Around 前 → ② @Before → [抛异常] → ②-异常 @AfterThrowing → ④ @After → @Around 异常处理
```

**本周验证（demo/aspect/TimeCostAspect.java）**：用 `@Around` 统计方法耗时 + `@AfterThrowing` 捕获异常通知。

### 2.3 事务传播行为速查

| 传播行为 | 当前有事务 | 当前无事务 |
|---|---|---|
| **REQUIRED** ★ | 加入 | 新建 |
| **REQUIRES_NEW** | 新建，挂起当前 | 新建 |
| NESTED | 嵌套（savepoint） | 新建 |
| SUPPORTS | 加入 | 非事务 |
| NOT_SUPPORTED | 挂起，非事务 | 非事务 |
| MANDATORY | 加入 | **抛异常** |
| NEVER | **抛异常** | 非事务 |

### 2.4 Bean 生命周期速查

```
实例化 → 属性填充 → Aware回调 → BP.before → @PostConstruct → afterPropertiesSet → init-method → BP.after(AOP) → 就绪 → @PreDestroy → destroy
```

### 2.5 自动装配核心注解

| 注解 | 条件 |
|---|---|
| `@ConditionalOnClass` | classpath 存在指定类 |
| `@ConditionalOnMissingBean` | 容器中无该 Bean |
| `@ConditionalOnProperty` | 配置匹配 |
| `@ConditionalOnBean` | 容器中有该 Bean |

---

## 三、事务失效案例清单

### 3.1 完整代码验证（transactionFailure/TransactionDemoService）

```java
// ❌ 失效1：private 方法
@Transactional(rollbackFor = Exception.class)
private void insertUser() { ... }    // 代理无法拦截 private

// ❌ 失效2：同类调用
public void callInsertUser() {
    this.insertUser();                // this 不是代理对象
}

// ❌ 失效3：catch 吞异常
@Transactional
public void insertUserWithCatch() {
    try { ... throw e; }
    catch (Exception e) { /* 吞了 */ }  // AOP 看不到异常
}
```

### 3.2 失效场景速查表

| 序号 | 场景 | 根因 | 解决 |
|---|---|---|---|
| 1 | private 方法 | 代理拦截不到 | 改成 public |
| 2 | 同类自调 | this 不走代理 | 抽到新 Service / AopContext |
| 3 | catch 吞异常 | AOP 感知不到 | catch 里重新 throw |
| 4 | 受检异常 | rollbackFor 默认 RuntimeException | `rollbackFor = Exception.class` |
| 5 | MyISAM | 引擎不支持事务 | 改用 InnoDB |
| 6 | 多线程 | 子线程不受管理 | 主线程内操作 |
| 7 | 非 Spring Bean | new 的不走代理 | @Autowired 注入 |

---

## 四、Spring 项目经验表达模板

### 4.1 日志切面实战（demo/aspect/LogAspect + TimeCostAspect）

```
"我做了一个日志切面系统：
- @Log 自定义注解 + @Around 切面，自动打印方法名/参数/返回值/耗时
- 配合 @AfterThrowing 捕获异常并记录错误日志
- 通过 @annotation 表达式精确拦截打了 @Log 的方法
- 这个切面在项目中避免了每个方法手动写 log.info/error，代码量减少了 70%"
```

**本周验证（demo 目录）**：`Log.java` 注解 → `LogAspect.java` 切面（@Around + @AfterThrowing）→ `LogTestController.java` 使用 → `TestController.java` 配合 `TimeCostAspect.java` 统计耗时。

### 4.2 自定义 Starter 实战（my_log_starter）

```
"我封装了一个 my-log-spring-boot-starter：
- 定义 LogService + LogAutoConfiguration（@ConditionalOnMissingBean）
- 在 AutoConfiguration.imports 文件中声明自动配置类
- 引入 jar 即可自动装配，无需手动 @ComponentScan
- 可通过 application.properties 控制开关"
```

### 4.3 事务失效排查实战（transactionFailure）

```
"我在项目中排查过 @Transactional 失效的问题：
- 现象：数据没回滚，异常被捕获了但数据库中已插入
- 排查：发现事务方法被同类内部调用（this.call()）
- 根因：this 调用不走代理，@Transactional 被忽略
- 解决：把事务方法抽到独立 Service 类中"
```

---

## 五、本周重点代码清单

| 序号 | 代码模块 | 关键文件 | 关键点 |
|---|---|---|---|
| 1 | IOC 容器层级 | W6D1_Actual | BeanFactory → ApplicationContext 完整继承链 |
| 2 | Bean 生命周期 | W6D2_Actual(待补充) | BeanPostProcessor + @PostConstruct + InitializingBean |
| 3 | AOP 自定义注解 | demo/anotation/Log.java | @Target(METHOD) + @Retention(RUNTIME) |
| 4 | AOP 切面 + 耗时统计 | demo/aspect/LogAspect + TimeCostAspect | @Around + @AfterThrowing 完整示例 |
| 5 | AOP 注解使用 | demo/controller/LogTestController + TestController | @Log 使用 + 切面拦截验证 |
| 6 | 事务失效三示例 | transactionFailure/TransactionDemoService | private/同类调用/catch吞异常 |
| 7 | 循环依赖 | W6D5_Actual(待补充) | 三级缓存 + A↔B 构造器报错 |
| 8 | 自定义 Starter | my_log_starter/ | @ConditionalOnMissingBean + AutoConfiguration.imports |
| 9 | Spring MVC 流程 | W6D5_Actual(待补充) | DispatcherServlet 五步 |
| 10 | 缓存一致性 | cache/AService + BService | 缓存+DB 双写验证 |

---

## 六、速记口诀

1. **IOC vs AOP**：IOC 管对象（容器帮你 new），AOP 管方法（代理帮你增强）
2. **注入三方式**：构造器推荐（final 不可变）> setter（可选依赖）> 字段（不推荐）
3. **Bean 生命周期**：实例化 → 填属性 → BP.before → 三初始化 → ★BP.after(AOP) → 就绪
4. **三级缓存**：一级成品、二级半成品、三级工厂；三级是为了 AOP 代理问题
5. **事务失效 5 必背**：private / 同类自调 / catch 吞 / rollbackFor / MyISAM
6. **传播行为 2 核心**：REQUIRED 同生共死，REQUIRES_NEW 各自独立
7. **自动装配**：EnableAutoConfiguration → 读取 imports 文件 → @Conditional 过滤 → 按需加载
8. **Spring MVC 五步**：HandlerMapping 定位 → HandlerAdapter 执行 → Controller 处理 → ViewResolver 解析 → 渲染
9. **6 种设计模式**：单例（Bean 默认）/ 工厂（BeanFactory）/ 代理（AOP）/ 模板（JdbcTemplate）/ 观察者（Event）/ 策略（DispatcherServlet）
10. **AOP 五通知顺序**：Around前 → Before → 目标 → AfterReturning → After → Around后
