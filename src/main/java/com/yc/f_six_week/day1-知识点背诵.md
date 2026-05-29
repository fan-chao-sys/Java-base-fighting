# Day1 IOC、DI、BeanDefinition、BeanFactory、ApplicationContext 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、IOC 与 DI 本质

### 1.1 核心定义

| 概念 | 一句话 | 类比 |
|---|---|---|
| **IOC（控制反转）** | 把对象的创建和管理交给 Spring 容器，而不是在代码中 `new` | 原来你买菜自己做，现在点外卖送到你手上 |
| **DI（依赖注入）** | IOC 的具体实现方式，容器自动把依赖"注入"到需要的地方 | 外卖小哥把饭送到你门口 |

**没有 IOC 时**：
```java
UserService service = new UserServiceImpl();  // 自己 new，硬编码，耦合死
```

**有了 IOC 后**：
```java
@Autowired
private UserService userService;  // 容器自动注入，解耦
```

### 1.2 DI 三种注入方式

| 方式 | 示例 | 推荐度 | 原因 |
|---|---|---|---|
| **构造器注入** | `public UserService(UserDao dao) { this.dao = dao; }` | ✅ **推荐** | 不可变(final)、不依赖反射、依赖显式暴露、测试友好 |
| **setter 注入** | `public void setDao(UserDao dao) { this.dao = dao; }` | ⚠️ 可选 | 适合可选依赖，但可能被覆盖 |
| **字段注入** | `@Autowired private UserDao dao;` | ❌ 不推荐 | 隐藏依赖、难测试、依赖容器 |

**构造器注入为什么最好？**
1. `final` 不可变 → 对象状态稳定
2. 构造时校验所有依赖 → 不会 NPE
3. 依赖太多一眼看出 → 类有 10 个构造参数说明要拆了
4. 单元测试不需要反射注入 mock

---

## 二、BeanDefinition —— Bean 的"身份证"

### 2.1 什么是 BeanDefinition

**BeanDefinition 是 Spring 对 Bean 的元数据描述**，定义了 Bean 的一切属性：

```
BeanDefinition {
    String beanClassName;      // 全限定类名
    String scope;              // singleton / prototype
    boolean lazyInit;          // 是否懒加载
    String initMethodName;     // 初始化方法
    String destroyMethodName;  // 销毁方法
    MutablePropertyValues pvs; // 属性值
    // ...
}
```

Spring 不是直接创建对象，而是先读取 BeanDefinition → 再根据它创建 Bean。

---

## 三、BeanFactory vs ApplicationContext

### 3.1 核心区别

| | BeanFactory | ApplicationContext |
|---|---|---|
| **层级** | 底层 IoC 容器接口 | 继承 BeanFactory，功能更丰富 |
| **加载时机** | **懒加载**：`getBean()` 时才创建 | **启动时全部单例 Bean 初始化**（Eager） |
| **功能** | Bean 的注册、获取 | + 事件发布 + 国际化 + 资源加载 + AOP + 环境抽象 |
| **使用场景** | 资源受限（如手机 App） | **企业应用默认使用** |

### 3.2 ApplicationContext 额外功能

```
ApplicationContext = BeanFactory + 事件机制(ApplicationEvent)
                    + 国际化(MessageSource)
                    + 资源加载(ResourceLoader)
                    + 环境抽象(Environment)
                    + AOP 支持
```

### 3.3 容器层级关系

```
BeanFactory（接口）
  └── ApplicationContext（接口，继承 BeanFactory）
        └── WebApplicationContext（Web 环境的上下文）
```

---

## 四、常见 IOC 注解速查

| 注解 | 作用 | 层级 |
|---|---|---|
| `@Component` | 通用组件注解，把类标记为 Spring Bean | 通用 |
| `@Service` | 标识 Service 层 Bean（语义化，本质同 @Component） | Service 层 |
| `@Repository` | 标识 DAO 层 Bean，额外提供持久层异常翻译 | DAO 层 |
| `@Controller` | 标识 MVC 控制器 Bean | Controller 层 |
| `@Configuration` | 标识配置类，内部 `@Bean` 方法生成 Bean | 配置 |
| `@Bean` | 方法级别的 Bean 声明，返回对象纳入容器管理 | 方法 |
| `@Autowired` | 按类型自动注入 | 注入 |
| `@Qualifier` | 配合 @Autowired 按名称指定 Bean | 注入 |
| `@Value` | 注入配置文件中的值 | 注入 |
| `@Scope` | 指定 Bean 作用域 | 作用域 |

---

## 五、Spring 容器启动流程

```
1. 读取配置（XML / 注解 / Java Config）
     ↓
2. 解析为 BeanDefinition，注册到 BeanDefinitionRegistry
     ↓
3. 执行 BeanFactoryPostProcessor（可修改 BeanDefinition，如 ${} 占位符替换）
     ↓
4. 遍历 BeanDefinition → 实例化 Bean → 填充属性 → 初始化
     ↓
5. 容器就绪，可提供 Bean
```

---

## 六、终极背诵总结

1. **IOC**：控制反转，对象创建交给容器，不要自己 new
2. **DI**：依赖注入，容器自动把依赖塞进去
3. **注入三方式**：构造器（推荐，final 不可变）> setter（可选依赖）> 字段注入（不推荐）
4. **BeanDefinition**：Bean 的元数据定义（class / scope / lazy / init-method / destroy-method）
5. **BeanFactory**：底层容器，懒加载，功能简单
6. **ApplicationContext**：高级容器，启动时初始化所有单例 Bean + 事件/国际化/资源/AOP
