import com.yc.z_common.Student;

import java.util.*;

public class W2D1_Actual {

    public static void main(String[] args) {
        // 写代码验证 Comparator 和 Comparable 同时存在时的排序优先级
        ArrayList<Student> list1 = new ArrayList<>();
        list1.add(new Student("张三", 20));
        list1.add(new Student("李四", 18));
        list1.add(new Student("王五", 22));

        // ==============================================
        // 重点：同时存在 Comparable + Comparator
        // 外部比较器：按【年龄 降序】排序
        // ==============================================
        Collections.sort(list1, new Comparator<Student>() {
            @Override
            public int compare(Student s1, Student s2) {
                System.out.println("=== 外部 Comparator 被调用 ===");
                return s2.getAge() - s1.getAge(); // 降序
            }
        });

        // 输出结果
        System.out.println("\n最终排序结果：");
        for (Student s : list1) {
            System.out.println(s.getName() + " - " + s.getAge() + "岁");
        }


      // 遍历 ArrayList 的 5 种方式（for、增强 for、迭代器、forEach、stream）并理解 fail-fas
        List<String> list = new ArrayList<>();
        list.add("A");
        list.add("B");
        list.add("C");
        list.add("D");
        System.out.println("===== 1. 普通 for 循环 =====");
        for (int i = 0; i < list.size(); i++) {
            System.out.println(list.get(i));
        }
        System.out.println("\n===== 2. 增强 for 循环 =====");
        for (String s : list) {
            System.out.println(s);
        }
        System.out.println("\n===== 3. 迭代器 Iterator =====");
        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next());
        }
        System.out.println("\n===== 4. forEach（JDK8+） =====");
        list.forEach(s -> System.out.println(s));
        System.out.println("\n===== 5. Stream 遍历 =====");
        list.stream().forEach(System.out::println);
    }

    // List、Set、Map 各自典型实现类和选型原则
    /**
     * List 系列
     * 实现类	    底层结构	核心特点
     * ArrayList	动态数组	查询快、增删慢；线程不安全；日常最常用
     * LinkedList	双向链表	头尾增删快、随机查询慢；线程不安全
     * Vector	    动态数组	线程安全；效率低，基本淘汰
     *
     * Set 系列
     * 实现类	     底层结构	        核心特点
     * HashSet	     HashMap	        无序、去重；查询 / 增删快；线程不安全
     * LinkedHashSet 有序哈希表	存取有序、去重；性能略低于 HashSet
     * TreeSet	     TreeMap	        自然排序 / 自定义排序；有序去重
     *
     * Map 系列
     * 实现类	         底层结构	                        核心特点
     * HashMap	         数组 + 链表 + 红黑树	无序；查询 /     增删极快；线程不安全；日常首选
     * LinkedHashMap	 同上 + 双向链表	存取有序；           可做 LRU 缓存
     * TreeMap	         红黑树	按键排序；                   查询略慢
     * HashTable	     数组 + 链表	                        线程安全；效率低，基本淘汰
     * ConcurrentHashMap 分段锁 / 数组 + 链表 + 红黑树	        线程安全、高并发首选
     *
     *  1. 选 List 场景
     *      优先          ArrayList
     *      绝大多数场景：查询多、遍历多、中间增删少。
     *      选           LinkedList
     *      频繁头部 / 尾部增删、极少随机查询。
     *      并发场景：自行加锁，不使用 Vector。
     *  2. 选 Set 场景（需求：去重）
     *      只要求去重、不关心顺序 →   HashSet
     *      要求去重 + 存取顺序一致 → LinkedHashSet
     *      要求去重 + 元素排序 →    TreeSet
     *  3. 选 Map 场景（需求：键值对）
     *      普通业务、单线程、不要求顺序 →  HashMap（首选）
     *      单线程、需要存取顺序 →        LinkedHashMap
     *      单线程、需要按键排序 →        TreeMap
     *      多线程 / 高并发 →            ConcurrentHashMap（必选）
     *      禁止使用 HashTable
     */


}