import com.yc.common.User;

import java.util.HashMap;

public class day1Actual {
    //💻 代码实战



    public static void compare(Integer aa, Integer bb, Integer cc) {
        System.out.println("-128 == -128：" + (aa == -128));
        System.out.println("127 == 127：" + (bb == 127));
        System.out.println("128 == 128：" + (cc == 128));
    }


    int a = 1;
    public void setA(int a) {
        System.out.println("a1:" + a);
        a = 20;
        System.out.println("a2:" + a);
    }

    public static void change(int num){
        num = 100; // 修改的是副本数值
    }

    public static void main(String[] args) {
//        new day1Actual().setA(5);

//        int a = 10;
//        change(a);
//        System.out.println(a); // 输出10，原变量无变化

        // 写一段代码验证 Integer 缓存池边界：用 == 比较 -128、127、128，观察结果
        Integer aa = -128;  // 缓存范围内
        Integer bb = 127;   // 缓存范围内（最大值）
        Integer cc = 128;   // 缓存范围外
        compare(aa,bb,cc);

        // 手写一个类重写 equals 和 hashCode，放入 HashMap 验证：map.put(obj1, 1); map.get(obj2) 的行为
        HashMap<User, Integer> map = new HashMap<>();
        // 创建两个内容相同的对象
        User obj1 = new User("张三", 100);
        User obj2 = new User("张三", 100);
        // 放入 obj1
        map.put(obj1, 1);
        // 用 obj2 获取值
        Integer value = map.get(obj2);

        System.out.println("obj1 == obj2 ：" + (obj1 == obj2)); // false（地址不同）
        System.out.println("obj1.equals(obj2) ：" + obj1.equals(obj2)); // true（内容相同）
        System.out.println("map.get(obj2) 得到的值：" + value); // 1
    }


}