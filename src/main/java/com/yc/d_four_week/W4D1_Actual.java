import org.openjdk.jol.info.ClassLayout;

public class W4D1_Actual {

    // 用 jol-core 工具打印一个对象的完整内存布局
    public static void main(String[] args) {
        // 打印 Object 对象布局（最基础）
        Object obj = new Object();
        System.out.println(ClassLayout.parseInstance(obj).toPrintable());

        // 也可以打印自定义对象
        // User user = new User();
        // System.out.println(ClassLayout.parseInstance(user).toPrintable());
    }

    static class User {
        int id;         // 4字节
        boolean flag;   // 1字节
        String name;    // 引用 4/8 字节
    }
}
