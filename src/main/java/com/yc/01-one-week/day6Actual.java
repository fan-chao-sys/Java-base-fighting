import com.yc.common.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class day6Actual{

    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException, InstantiationException {

        // 用反射创建一个对象并调用其 private 方法
        // 1. 获取 Class 对象
        Class<User> clazz = User.class;
        // 2. 调用 private 构造方法创建对象
        Constructor<User> constructor = clazz.getDeclaredConstructor(String.class);
        constructor.setAccessible(true); // 绕过访问权限检查
        User user = constructor.newInstance("小明");
        // 3. 获取 private 方法
        Method method = clazz.getDeclaredMethod("sayHello", String.class);
        method.setAccessible(true); // 绕过访问权限检查
        // 4. 调用 private 方法
        method.invoke(user, "欢迎学习反射！");

        // 写一个 JDK 动态代理示例：对接口方法调用前后打印日志
        // 目标对象
        UserService target = new UserServiceImpl();
        // 生成代理对象
        UserService proxyInstance = (UserService) Proxy.newProxyInstance(
                target.getClass().getClassLoader(),    // 类加载器
                target.getClass().getInterfaces(),      // 目标对象实现的接口
                new LogHandler(target)                  // 自定义处理器
        );
        // 调用代理方法
        proxyInstance.addUser("张三");
        System.out.println("---------------------");
        proxyInstance.deleteUser(1001);



    }

    // 实现一个简单的单例模式（推荐双重检查锁版本） Singleton


    /**
     * 笔记：Java 常见注解及作用（@Override、@Deprecated、@SuppressWarnings、@FunctionalInterface）
     *
     * 1.@Overrid                       作用：标识重写父类 / 接口方法，编译校验方法签名是否匹配，写错直接报错。
     * 2.@Deprecated                    作用：标记类、方法、字段已过时，提醒不再推荐使用。
     * 3.@SuppressWarnings              作用：压制编译警告
     * 4.@SuppressWarnings("all")       作用：屏蔽全部警告
     * 5.@SuppressWarnings("unchecked") 作用：屏蔽泛型未检查警告
     * 6.@FunctionalInterface           作用：限定接口只能有一个抽象方法，可用于 Lambda 表达式、函数式编程。
     *
     */

    /**|
     * 反射执行流程简图:
     *
     *  获取Class字节码对象
     *         ↓
     *  获取构造器/方法/字段
     *         ↓
     *  setAccessible(true) 破除权限
     *         ↓
     *  创建实例、调用方法、修改属性
     */

    /**
     * JDK 动态代理流程图:
     *
     *  客户端 → 代理对象
     *            ↓
     *   InvocationHandler.invoke()
     *     前置增强(日志/校验)
     *            ↓
     *    反射调用目标方法
     *            ↓
     *    后置增强(统计/收尾)
     *            ↓
     *   返回结果给客户端
     */

    /**
     * 层级关系极简图:
     *
     *  业务接口
     *    ↗       ↘
     * 目标实现类   代理Proxy类
     *    ↖       ↗
     *     调用处理器
     *     (反射执行)
     */
}