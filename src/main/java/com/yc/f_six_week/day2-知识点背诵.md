# Day2 Bean 生命周期、后置处理器、作用域 底层核心知识点背诵笔记 ⭐⭐⭐

---

## 一、Bean 完整生命周期（面试必背全链路）

```
1. 实例化（Instantiation）
   BeanDefinition → createBeanInstance() → 反射 new Instance（对象已创建，属性全为 null）

2. 属性填充（Populate）
   populateBean() → 依赖注入（@Autowired / @Value）

3. Aware 回调
   BeanNameAware.setBeanName()            → 知道自己的 Bean 名称
   BeanFactoryAware.setBeanFactory()      → 拿到 BeanFactory
   ApplicationContextAware.setApplicationContext()

4. 初始化前 —— BeanPostProcessor.before
   postProcessBeforeInitialization()      → 可修改 Bean 属性、@Value 解析

5. 初始化
   @PostConstruct                          → 最先执行
   InitializingBean.afterPropertiesSet()   → 其次
   init-method（XML / @Bean(initMethod)）   → 最后

6. 初始化后 —— BeanPostProcessor.after
   postProcessAfterInitialization()        → ★ AOP 在此生成代理对象！

7. Bean 就绪
   放入 singletonObjects（一级缓存），对外可用

8. 销毁
   @PreDestroy
   DisposableBean.destroy()
   destroy-method
```

**口诀**：实例化 → 填属性 → 知身份 → before → 三初始化 → after 生成代理 → 就绪 → 销毁

---

## 二、初始化三个钩子的执行顺序

```
同一个 Bean 中：
  @PostConstruct          ← 第1优先级（javax.annotation）
  ↓
  InitializingBean.afterPropertiesSet()  ← 第2优先级（Spring 接口）
  ↓
  init-method             ← 第3优先级（XML / @Bean(initMethod) 配置）
```

**销毁同理**：`@PreDestroy` → `DisposableBean.destroy()` → `destroy-method`

---

## 三、BeanPostProcessor —— AOP 的根基

### 3.1 核心作用

```java
public interface BeanPostProcessor {
    // 初始化前：可以修改 Bean 属性
    default Object postProcessBeforeInitialization(Object bean, String beanName) {
        return bean;
    }

    // 初始化后：可以返回代理对象（AOP 就是在这里替换的）
    default Object postProcessAfterInitialization(Object bean, String beanName) {
        return bean;  // 返回代理对象替代原 Bean
    }
}
```

### 3.2 Spring 内置的 BeanPostProcessor

| 实现类 | 作用 |
|---|---|
| `AutowiredAnnotationBeanPostProcessor` | 处理 `@Autowired` 和 `@Value` 注入 |
| `CommonAnnotationBeanPostProcessor` | 处理 `@PostConstruct` / `@PreDestroy` |
| `AnnotationAwareAspectJAutoProxyCreator` | ★ **AOP 代理创建** |

### 3.3 AOP 代理在哪个环节生成？

> **`BeanPostProcessor.postProcessAfterInitialization` → 检查是否有切面匹配 → 有则创建代理对象替代原 Bean**

---

## 四、Bean 作用域

| 作用域 | 含义 | 生命周期 |
|---|---|---|
| **singleton**（默认） | IoC 容器中**只有一个实例** | 容器启动创建，关闭销毁 |
| **prototype** | 每次获取**创建新实例** | 容器只管创建不管销毁 |
| **request** | 每个 HTTP 请求一个实例 | 请求结束销毁 |
| **session** | 每个 HTTP Session 一个实例 | Session 过期销毁 |
| **application** | 一个 ServletContext 一个实例 | 同 singleton（Web 维度） |

**prototype 注意**：Spring 不管理 prototype Bean 的完整生命周期，`@PreDestroy` 不会自动调用。

---

## 五、BeanFactoryPostProcessor —— 改 Bean 定义的

区别于 BeanPostProcessor（改 Bean 实例），BeanFactoryPostProcessor 在**所有 BeanDefinition 加载后、实例化前**执行：

```java
// 典型实现：PropertySourcesPlaceholderConfigurer
// 把 BeanDefinition 中的 ${jdbc.url} 替换为 application.properties 中的实际值
```

**两者区别**：

| | BeanFactoryPostProcessor | BeanPostProcessor |
|---|---|---|
| **时机** | BeanDefinition 加载后、实例化前 | 每个 Bean 初始化前后 |
| **操作对象** | **BeanDefinition**（元数据） | **Bean 实例** |
| **AOP 相关** | 否 | 是 |

---

## 六、终极背诵总结

1. **生命周期主线**：实例化 → 填属性 → Aware → BP.before → @PostConstruct → afterPropertiesSet → init-method → BP.after(AOP代理) → 就绪
2. **BeanPostProcessor**：before 改属性，after 生成代理（AOP 根基）
3. **BeanFactoryPostProcessor**：在实例化前改 BeanDefinition（如 ${} 占位符替换）
4. **三个初始化钩子**：@PostConstruct → InitializingBean.afterPropertiesSet → init-method
5. **singleton vs prototype**：单例容器管理全程，多例容器只管创建不销毁
6. **AOP 代理在那步**：BeanPostProcessor.postProcessAfterInitialization
