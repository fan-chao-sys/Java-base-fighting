# Day4 Spring 事务原理、传播行为、隔离级别、事务失效场景 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、Spring 事务原理

### 1.1 核心流程

```
@Transactional
public void transfer() {                           ← 业务方法
    // ...
}
        ↓  AOP 拦截（TransactionInterceptor）
    ┌───────────────────┐
    │ 1. 开启事务        │  PlatformTransactionManager.getTransaction()
    │ 2. 执行业务        │  method.invoke()
    │ 3. 正常→提交       │  txManager.commit()
    │    异常→回滚       │  txManager.rollback()
    └───────────────────┘
```

**一句话**：通过 AOP 切面 + `PlatformTransactionManager`，在方法前后开启/提交/回滚事务。

---

## 二、7 种传播行为（Propagation）⭐⭐⭐

| 传播行为 | 当前有事务 | 当前无事务 |
|---|---|---|
| **REQUIRED**（默认） | **加入**当前事务 | **新建**一个事务 |
| **REQUIRES_NEW** | **新建**事务，**挂起**当前事务 | **新建**一个事务 |
| **NESTED** | 创建**嵌套事务**（savepoint 回滚点） | 同 REQUIRED |
| **SUPPORTS** | 加入当前事务 | **非事务**执行 |
| **NOT_SUPPORTED** | **挂起**当前事务，非事务执行 | 非事务执行 |
| **MANDATORY** | 加入当前事务 | **抛异常** |
| **NEVER** | **抛异常** | 非事务执行 |

### 2.1 最重要的两个

| | REQUIRED | REQUIRES_NEW |
|---|---|---|
| **有事务时** | 加入已有事务（同生共死） | **新建独立事务**（互不影响） |
| **回滚影响** | 一个回滚全部回滚 | 各自独立回滚 |
| **典型场景** | 订单扣库存（整体成功或失败） | 记录操作日志（主业务失败日志也要保留） |

**REQUIRED 回滚示例**：
```
A.required() → B.required()
B 抛异常 → 整个事务回滚 → A 也回滚（同生共死）
```

**REQUIRES_NEW 回滚示例**：
```
A.required() → B.requiresNew()
B 抛异常 → B 回滚，A 不受影响（各自独立）
```

---

## 三、事务失效 6 大场景（面试必背）⭐⭐⭐

| 序号 | 场景 | 原因 | 代码示例 |
|---|---|---|---|
| ① | **方法不是 public** | CGLIB/JDK 代理只能拦截 public 方法 | `private void transfer()` |
| ② | **同类内部调用** | `this.method()` 不走代理，直接调用的是原始对象 | `this.transfer()` |
| ③ | **异常被 catch 吞掉** | AOP 只看**未被捕获的异常** | `catch (Exception e) { /* 吞了 */ }` |
| ④ | **rollbackFor 未配置** | 默认只回滚 `RuntimeException` 和 `Error`，受检异常不回滚 | `@Transactional` 不加 rollbackFor |
| ⑤ | **数据库引擎不支持事务** | MyISAM 不支持事务，加 `@Transactional` 也没用 | `ENGINE=MyISAM` |
| ⑥ | **多线程中调用** | 子线程的事务不受 Spring 管理 | `new Thread(() -> transfer()).start()` |

### 3.1 同类内部调用为什么失效？

```java
@Service
public class OrderService {

    @Transactional
    public void methodA() {     // 外部调用 → 走代理 → 事务生效
        methodB();              // 内部调用 → this.methodB() → 不走代理 → 事务失效！
    }

    @Transactional
    public void methodB() { }
}
```

**解决**：
1. 把 `methodB` 挪到另一个 Service
2. `AopContext.currentProxy()` 获取当前代理对象调用
3. `@Autowired private OrderService self; self.methodB();`

### 3.2 为什么受检异常默认不回滚？

Spring 认为**受检异常属于业务异常**（如文件不存在），应该由开发者显式处理，不应自动回滚。要回滚受检异常需加 `rollbackFor = Exception.class`。

---

## 四、事务隔离级别

| 隔离级别 | 脏读 | 不可重复读 | 幻读 |
|---|---|---|---|
| **DEFAULT** | 跟随数据库（MySQL→RR） | — | — |
| **READ_UNCOMMITTED** | ✅ | ✅ | ✅ |
| **READ_COMMITTED** | ❌ | ✅ | ✅ |
| **REPEATABLE_READ** | ❌ | ❌ | ⚠️ |
| **SERIALIZABLE** | ❌ | ❌ | ❌ |

```java
@Transactional(isolation = Isolation.REPEATABLE_READ)  // 一般不轻易改
```

---

## 五、终极背诵总结

1. **事务原理**：`@Transactional` = AOP + `TransactionInterceptor` + `PlatformTransactionManager`
2. **传播行为 2 个最重要**：REQUIRED（默认，同生共死）、REQUIRES_NEW（各自独立）
3. **事务失效 6 场景**：非 public / 同类自调 / catch 吞异常 / rollbackFor 未配 / MyISAM / 多线程
4. **受检异常不回滚**：Spring 认为受检异常是业务的，需显式 `rollbackFor`
5. **同类自调解决**：抽到新 Service / `AopContext.currentProxy()` / 注入 self
