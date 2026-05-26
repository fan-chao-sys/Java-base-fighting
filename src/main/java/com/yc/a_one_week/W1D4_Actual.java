// 写一个 try-with-resources 示例，实现 AutoCloseable 接口观察资源释放顺序

import com.yc.Xcommon.Resource;

public class W1D4_Actual {

    public static void main(String[] args) {
        // try 后面声明多个资源 → 自动关闭
        try (Resource resource1 = new Resource("资源1");
             Resource resource2 = new Resource("资源2")) {

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

