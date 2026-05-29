import com.yc.z_common.User;

import java.util.HashMap;
import java.util.Map;

public class W2D4_Actual {


    public static void main(String[] args) {

        // 手写代码验证：自定义类作为 key 时，hashCode 方法对 HashMap 存取的影响
        Map<User, String> map = new HashMap<>();

        // 两个内容相同的对象
        User u1 = new User("张三", 18);
        User u2 = new User("张三", 18);
        // 存入 u1
        map.put(u1, "JAVA");
        // 尝试用 u2 获取
        System.out.println("u1.equals(u2) = " + u1.equals(u2)); // true
        System.out.println("map.get(u2) = " + map.get(u2));     // null（重点！）
        System.out.println("map.size() = " + map.size());       // 1
        // 再 put u2
        map.put(u2, "SPRING");
        System.out.println("map.size() = " + map.size());      // 2（两个相同内容对象存成了两个 key）
    }
}