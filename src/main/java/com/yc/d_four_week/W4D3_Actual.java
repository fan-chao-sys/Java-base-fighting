import com.yc.z_common.MyCustomClassLoader;

public class W4D3_Actual {

    public static void main(String[] args) throws ClassNotFoundException {
        // 写代码打印三个类加载器的加载路径：BootstrapClassLoader / PlatformClassLoader / AppClassLoader
        // 1. 获取应用类加载器（AppClassLoader）
        ClassLoader appClassLoader = ClassLoader.getSystemClassLoader();
        System.out.println("===== 1. 应用类加载器（AppClassLoader）=====");
        System.out.println("加载器实例：" + appClassLoader);
        printURLs("加载路径", appClassLoader);

        // 2. 获取平台类加载器（PlatformClassLoader / ExtClassLoader）
        ClassLoader platformClassLoader = appClassLoader.getParent();
        System.out.println("\n===== 2. 平台/扩展类加载器（Platform/Ext）=====");
        System.out.println("加载器实例：" + platformClassLoader);
        printURLs("加载路径", platformClassLoader);

        // 3. 获取启动类加载器（BootstrapClassLoader，C++实现，Java中为null）
        ClassLoader bootstrapClassLoader = platformClassLoader.getParent();
        System.out.println("\n===== 3. 启动类加载器（BootstrapClassLoader）=====");
        System.out.println("加载器实例：" + bootstrapClassLoader + " （C++实现，Java层显示null）");
        printBootstrapPaths();


        // 自定义一个类加载器，从指定目录加载 class 文件
        // 指定你要加载的 class 所在目录
        String loadPath = "D:\\Java\\ideaworkspare\\dev\\Java-fighting\\src\\main\\java\\com\\yc\\Xcommon\\User.java";
        // 创建自定义类加载器
        MyCustomClassLoader classLoader = new MyCustomClassLoader(loadPath);
        // 加载指定类（必须写全类名）
        Class<?> clazz = classLoader.loadClass("com.example.MyClass");
        // 查看结果
        System.out.println("加载的类：" + clazz.getName());
        System.out.println("类加载器：" + clazz.getClassLoader());
    }



    /**
     * 打印 ClassLoader 加载的路径（URLClassLoader 专用）
     */
    private static void printURLs(String title, ClassLoader classLoader) {
        if (classLoader == null) {
            System.out.println(title + "：无（Bootstrap 加载器）");
            return;
        }

        // App/Platform 都是 URLClassLoader 子类
        java.net.URLClassLoader urlClassLoader = (java.net.URLClassLoader) classLoader;
        java.net.URL[] urls = urlClassLoader.getURLs();
        for (java.net.URL url : urls) {
            System.out.println("  - " + url.getPath());
        }
    }

    /**
     * 打印 BootstrapClassLoader 加载的核心路径
     */
    private static void printBootstrapPaths() {
        String bootstrapPaths = System.getProperty("sun.boot.class.path"); // JDK8
        String modulePaths = System.getProperty("jdk.module.path");        // JDK9+

        System.out.println("JDK8 Bootstrap 加载路径（sun.boot.class.path）：");
        if (bootstrapPaths != null) {
            for (String path : bootstrapPaths.split(System.getProperty("path.separator"))) {
                System.out.println("  - " + path);
            }
        } else {
            System.out.println("  - JDK9+ 模块化环境，无此属性");
        }

        System.out.println("\nJDK9+ 模块路径（jdk.module.path）：");
        if (modulePaths != null) {
            for (String path : modulePaths.split(System.getProperty("path.separator"))) {
                System.out.println("  - " + path);
            }
        }
    }

}
