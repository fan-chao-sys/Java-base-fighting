# Day6 Spring Boot 自动装配原理、设计模式 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、Spring Boot 自动装配原理 ⭐⭐⭐

### 1.1 核心入口

```
@SpringBootApplication
  └── @EnableAutoConfiguration        ← 自动装配的总开关
        └── @Import(AutoConfigurationImportSelector.class)
              └── 读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
                    └── 按 @Conditional 条件过滤
                          └── 加载符合条件的自动配置类
```

### 1.2 装配文件位置变化

| Spring Boot 版本 | 文件 | 格式 |
|---|---|---|
| **2.x 及以前** | `META-INF/spring.factories` | key-value 格式：`EnableAutoConfiguration=\ ...` |
| **3.x 及以后** | `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` | 每行一个全类名 |

### 1.3 核心条件注解

| 注解 | 条件 | 典型用法 |
|---|---|---|
| `@ConditionalOnClass` | classpath 中**存在**指定类 | 有 `DataSource` 类才装配数据源 |
| `@ConditionalOnMissingBean` | 容器中**没有**该 Bean | 用户没自定义 DataSource 时才自动配 |
| `@ConditionalOnProperty` | 配置项**匹配**时才生效 | `spring.cache.type=redis` |
| `@ConditionalOnBean` | 容器中有指定 Bean | 有 DataSource 才配 JdbcTemplate |
| `@ConditionalOnMissingClass` | classpath 中**不存在**指定类 | — |

### 1.4 自动装配举例

```java
// 场景：classpath 中有 HikariCP（spring-boot-starter-data-jpa 自带）
// 配置了 spring.datasource.url，但没有自定义 DataSource Bean

@Configuration
@ConditionalOnClass(DataSource.class)             // classpath 有 DataSource
@ConditionalOnMissingBean(DataSource.class)       // 用户没自定义 DataSource
@EnableConfigurationProperties(DataSourceProperties.class)
public class DataSourceAutoConfiguration {
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSource dataSource() {
        return new HikariDataSource();            // 自动创建 HikariCP 数据源
    }
}
```

**一句话概括自动装配**：根据 classpath 的 jar 依赖和配置文件，**按需自动创建 Bean**。

---

## 二、设计模式在 Spring 中的应用

| 设计模式 | Spring 中的体现 | 一句话 |
|---|---|---|
| **单例模式** | Bean 默认作用域 `singleton` | 每个 Bean 只有一个实例 |
| **工厂模式** | `BeanFactory` / `ApplicationContext` | 把对象的创建和使用分离 |
| **代理模式** | **AOP** — JDK 动态代理 / CGLIB | 在不改原方法的情况下增强功能 |
| **模板方法** | `JdbcTemplate` / `RedisTemplate` / `RestTemplate` | 固定流程（获取连接→执行→释放），子步骤可变 |
| **观察者模式** | `ApplicationEvent` + `@EventListener` | Bean A 发事件，Bean B/C/D 自动收到 |
| **策略模式** | `DispatcherServlet` 的 `HandlerMapping` 选择 Handler | 根据 URL 选择不同的 Controller |
| **责任链模式** | Filter / Interceptor 链 | 请求逐个经过 Filter → DispatcherServlet → Interceptor → Controller |

### 2.1 各模式详解

**模板方法（JdbcTemplate）**：
```
JdbcTemplate.query(sql, rowMapper)
  → ① 获取连接（固定）
  → ② 创建 PreparedStatement（固定）
  → ③ 你的 rowMapper 处理每行（可变 ← 这就是模板的"钩子"）
  → ④ 释放连接（固定）
```

**观察者模式（ApplicationEvent）**：
```java
// 发布事件
context.publishEvent(new OrderCreatedEvent(order));

// 监听事件
@EventListener
public void handleOrderCreated(OrderCreatedEvent event) {
    // 自动收到事件通知，无需显式注册
}
```

**策略模式（DispatcherServlet）**：
```
请求 GET /users/1 → RequestMappingHandlerMapping 匹配到 UsersController.getUser()
请求 POST /users → 匹配到 UsersController.createUser()
同一个 DispatcherServlet，根据 URL 选择不同的处理策略
```

---

## 三、自定义 Starter 开发要点

```java
// 1. 自动配置类
@Configuration
@ConditionalOnClass(MyService.class)
@EnableConfigurationProperties(MyProperties.class)
public class MyAutoConfiguration {
    @Bean
    @ConditionalOnMissingBean
    public MyService myService(MyProperties props) {
        return new MyServiceImpl(props);
    }
}

// 2. 配置属性类
@ConfigurationProperties(prefix = "my.starter")
public class MyProperties {
    private String url;
    private int timeout = 3000;
    // getter/setter
}
```

```
// 3. AutoConfiguration.imports 文件
// META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.example.MyAutoConfiguration
```

---

## 四、终极背诵总结

1. **自动装配入口**：`@SpringBootApplication` → `@EnableAutoConfiguration` → `AutoConfigurationImportSelector`
2. **装配文件**：Boot 3.x 用 `AutoConfiguration.imports`，2.x 用 `spring.factories`
3. **核心条件注解**：`@ConditionalOnClass` / `@ConditionalOnMissingBean` / `@ConditionalOnProperty`
4. **一句话**：根据 classpath 的 jar 和配置，按需自动创建 Bean
5. **6 种设计模式**：单例（Bean 默认）、工厂（BeanFactory）、代理（AOP）、模板方法（JdbcTemplate）、观察者（ApplicationEvent）、策略（DispatcherServlet）
