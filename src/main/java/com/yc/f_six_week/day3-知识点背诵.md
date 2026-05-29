# Day3 AOP、动态代理、切面、通知、切点 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、AOP 核心概念速查

### 1.1 五个核心术语

| 术语 | 含义 | 类比 |
|---|---|---|
| **切面（Aspect）** | 横切关注点的模块化，= 通知 + 切点 | "日志切面"包含"在哪切"和"切了干什么" |
| **通知（Advice）** | 切面在特定连接点执行的**具体动作** | "方法执行前打印日志" |
| **切点（Pointcut）** | 匹配哪些方法被拦截的**表达式规则** | `execution(* com.yc.service.*.*(..))` |
| **连接点（JoinPoint）** | 程序执行中的某个点（方法是 Spring AOP 仅支持的类型） | 被拦截的方法 |
| **织入（Weaving）** | 将切面应用到目标对象并创建代理的过程 | 编译期/类加载期/运行期（Spring AOP 是运行期） |

### 1.2 AOP 解决了什么问题

```
原始代码：                    使用 AOP：
public void transfer() {     public void transfer() {
    logStart();               // 只有业务逻辑
    // 业务逻辑                 @Transactional
    logEnd();                 @Log
}                            }
                             日志和事务被"切面"统一管理
```

**核心价值**：把横切关注点（日志/事务/鉴权/缓存）从业务代码中分离 → **高内聚低耦合**。

---

## 二、5 种通知（Advice）执行顺序 ⭐⭐

```java
@Aspect
@Component
public class LogAspect {

    @Around("execution(* com.yc.service.*.*(..))")
    public Object around(ProceedingJoinPoint pjp) throws Throwable {
        System.out.println("① @Around 前");          // 1. 环绕前
        Object result = pjp.proceed();               // 2. 执行目标方法（含 Before）
        System.out.println("⑤ @Around 后");          // 5. 环绕后
        return result;
    }

    @Before("execution(* com.yc.service.*.*(..))")
    public void before() { System.out.println("② @Before"); }

    @AfterReturning("execution(* com.yc.service.*.*(..))")
    public void afterReturning() { System.out.println("③ @AfterReturning"); }

    @After("execution(* com.yc.service.*.*(..))")
    public void after() { System.out.println("④ @After"); }

    @AfterThrowing("execution(* com.yc.service.*.*(..))")
    public void afterThrowing() { System.out.println("②-异常 @AfterThrowing"); }
}
```

### 2.1 正常执行顺序

```
① @Around 前 → ② @Before → [目标方法] → ③ @AfterReturning → ④ @After → ⑤ @Around 后
```

### 2.2 异常执行顺序

```
① @Around 前 → ② @Before → [目标方法抛异常] → ②-异常 @AfterThrowing → ④ @After → @Around 异常处理
```

### 2.3 五通知对比

| 通知 | 执行时机 | 能否获取返回值 | 能否处理异常 | 能否阻止方法执行 |
|---|---|---|---|---|
| **@Before** | 方法前 | ❌ | ❌ | ❌ |
| **@AfterReturning** | 方法正常返回后 | ✅ `returning` | ❌ | ❌ |
| **@AfterThrowing** | 方法抛异常后 | ❌ | ✅ `throwing` | ❌ |
| **@After** | 方法后（无论正常/异常） | ❌ | ❌ | ❌ |
| **@Around** | 包围方法 | ✅ | ✅ | ✅（不调 proceed） |

---

## 三、Spring AOP 底层实现

### 3.1 两种代理方式

| | JDK 动态代理 | CGLIB |
|---|---|---|
| **原理** | 基于接口，`Proxy` + `InvocationHandler` | 基于继承，ASM 生成子类 |
| **限制** | **必须有接口** | 不能代理 **final** 类/方法 |
| **默认** | Spring Boot 1.x 默认（有接口时） | **Spring Boot 2.x 默认** |
| **配置** | `spring.aop.proxy-target-class=false` | `spring.aop.proxy-target-class=true` |

### 3.2 Spring Boot 2.x 为什么默认用 CGLIB？

- 不需要强制写接口，对代码侵入性更小
- 避免了"注入的 Bean 是 JDK 代理时，调用方必须引用接口而非实现类"的问题
- 但 final 方法仍然无法增强

### 3.3 @Transactional 原理

```java
// @Transactional 是 AOP 最经典的用法
@Transactional
public void transfer() {
    // AOP 实际做的事：
    // ① TransactionInterceptor 拦截这个方法
    // ② 方法前：开启事务
    // ③ 执行业务代码
    // ④ 正常 → 提交事务 / 异常 → 回滚事务
}
```

---

## 四、自定义注解 + 切面实战

```java
// 1. 定义自定义注解
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Log {
    String value() default "";
}

// 2. 写切面
@Aspect @Component
public class LogAspect {
    @Around("@annotation(log)")
    public Object around(ProceedingJoinPoint pjp, Log log) {
        System.out.println("[Log] " + log.value() + " 开始");
        Object result = pjp.proceed();
        System.out.println("[Log] " + log.value() + " 结束");
        return result;
    }
}

// 3. 使用
@Log("转账操作")
public void transfer() { ... }
```

---

## 五、AOP 和 IOC 的关系

> **IOC 是地基，AOP 是地基上的一种横切能力。**
> IOC 管理了 Bean，AOP 在 Bean 的初始化阶段（`BeanPostProcessor.after`）用代理包装这些 Bean，拦截方法调用织入通知。

---

## 六、终极背诵总结

1. **AOP 五术语**：切面（通知+切点）、通知（执行动作）、切点（匹配规则）、连接点（被拦截方法）、织入（生成代理）
2. **五通知执行顺序**：Around前 → Before → 目标 → Around后/AfterReturning → After → Around后
3. **异常顺序**：Around前 → Before → 异常 → AfterThrowing → After → Around异常处理
4. **Spring AOP 底层**：有接口用 JDK 代理，无接口 / Boot 2.x 默认 CGLIB
5. **@Transactional 原理**：TransactionalInterceptor 通过 AOP 在方法前后开启/提交/回滚事务
6. **AOP 与 IOC 关系**：IOC 管 Bean → BeanPostProcessor 在初始化后生成代理 → AOP
