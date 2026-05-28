public class W4D2_Actual {

    // 小对象，完全不会逃逸
    static class User {
        private int id;     // 标量
        private String name;// 标量
    }

    // 写一段可触发栈上分配的代码，用 -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCDetails 对比开/关逃逸分析前后的 GC 情况
    public static void main(String[] args) {
        long start = System.currentTimeMillis();

        // 循环创建 1亿 个小对象
        for (int i = 0; i < 100_000_000; i++) {
            createUser();
        }

        System.out.println("执行耗时：" + (System.currentTimeMillis() - start) + "ms");
    }

    // 关键：User 对象只在这个方法里创建和使用，没有逃逸！
    private static void createUser() {
        // 这个对象 100% 未逃逸 → 满足栈上分配条件
        User user = new User();
        user.id = 1;
        user.name = "test";
    }
}
