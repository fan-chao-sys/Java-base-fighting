import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

public class W4D4_Actual {

    public static void main(String[] args) {
        // 写一个触发 OOM 的代码，分别用软引用和弱引用来验证回收行为

        /**
         * 强引用：持续创建对象，堆满直接 OOM
         * JVM 参数：-Xms20m -Xmx20m -XX:+PrintGCDetails
         */
        // ====== 强引用测试（取消注释运行）======
        // List<Object> list1 = new ArrayList<>();
        // int i = 0;
        // while (true) {
        //     list1.add(new byte[1024 * 1024]);
        //     System.out.println("创建第 " + (++i) + " 个 1M 数组");
        // }

        // ====== 软引用测试（默认运行）======
        // JVM 参数：-Xms20m -Xmx20m -XX:+PrintGCDetails
        List<SoftReference<byte[]>> list = new ArrayList<>();
        int j = 0;
        while (true) {
            byte[] bytes = new byte[1024 * 1024];
            SoftReference<byte[]> softRef = new SoftReference<>(bytes);
            list.add(softRef);
            System.out.println("创建第 " + (++j) + " 个 1M 数组");
            bytes = null;
        }

    }
}
