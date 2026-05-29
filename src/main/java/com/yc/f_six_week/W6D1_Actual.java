public class W6D1_Actual {

    public static void main(String[] args) {
        /**
         *
         * BeanFactory (顶层接口，定义基础 Bean 管理)
         *     ↑ 继承
         * ListableBeanFactory
         *     ↑ 继承
         * HierarchicalBeanFactory
         *     ↑ 组合
         * ConfigurableBeanFactory
         *     ↑ 实现
         * AbstractBeanFactory
         *     ↑ 继承
         * ConfigurableListableBeanFactory
         *     ↑ 实现
         * AbstractAutowireCapableBeanFactory
         *     ↑ 继承
         * DefaultListableBeanFactory (基础实现)
         *     ↑ 扩展
         * ApplicationContext (子接口，BeanFactory 的超集)
         *     ├── ConfigurableApplicationContext
         *     │   └── AbstractApplicationContext
         *     │       ├── GenericApplicationContext
         *     │       └── AbstractRefreshableConfigApplicationContext
         *     │           └── AbstractRefreshableWebApplicationContext
         *     │               └── WebApplicationContext (Web 环境专用容器)
         *     │                   └── ServletWebServerApplicationContext (Spring Boot 常用)
         *     ├── ClassPathXmlApplicationContext (传统 XML 方式)
         *     └── AnnotationConfigApplicationContext (注解配置方式)
         *
         */
    }
}
