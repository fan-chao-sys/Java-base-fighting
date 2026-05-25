// 写一个 try-with-resources 示例，实现 AutoCloseable 接口观察资源释放顺序

public class day4Actual implements AutoCloseable {
    private String name;

    public void day4Actual(String name) {
        this.name = name;
        System.out.println(name + "：创建成功");
    }

    // 必须实现的关闭方法
    @Override
    public void close() throws Exception {
        System.out.println(name + "：自动关闭（释放资源）");
    }

    // 模拟业务方法
    public void doSomething() {
        System.out.println(name + "：执行业务逻辑");
    }

    public static void main(String[] args) {
        // try 后面声明多个资源 → 自动关闭
        try (day4Actual resource1 = new day4Actual("资源1");
             day4Actual resource2 = new day4Actual("资源2")) {

            resource1.doSomething();
            resource2.doSomething();
            System.out.println("=== 业务执行完毕 ===");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 结果:
        //    资源1：创建成功
        //    资源2：创建成功
        //    资源1：执行业务逻辑
        //    资源2：执行业务逻辑
        //      === 业务执行完毕 ===
        //    资源2：自动关闭（释放资源）
        //    资源1：自动关闭（释放资源）
}

