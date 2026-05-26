import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class W2D3_Actual {
    private static final int COUNT = 100_000; // 10万条数据

    /**
     * 问：ArrayList vs LinkedList 5 维度对比表（底层结构、查询、增删、内存、遍历方式）？
     *
     * 对比维度	    ArrayList	                                                    LinkedList
     * 底层结构	    动态数组（Object []）	                                            双向链表（Node 节点）
     * 查询（get）	支持随机访问，O (1)，极快	                                        不支持随机访问，需从头遍历，O (n)，慢
     * 增删（add/remove）	中间 / 头部增删需要移动元素，O (n)；尾部增删快	             头部 / 中间增删只需改指针，O (1)；任意位置都高效
     * 内存占用	    连续内存，会预留扩容空间，内存占用少	                             每个节点存：数据 + 前驱 + 后继，内存开销更大
     * 遍历方式	    普通 for / 增强 for / 迭代器	                                 增强 for / 迭代器（禁止普通 for 遍历）
     *
     * @param args
     */

    public static void main(String[] args) {
        System.out.println("===== 插入 " + COUNT + " 条数据耗时对比（单位：毫秒）=====\n");

        // 1. 尾部插入
        testAddAtEnd();

        // 2. 中间插入
        testAddAtMiddle();

        // 3. 头部插入
        testAddAtHead();
    }

    // 尾部插入
    private static void testAddAtEnd() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();

        long start1 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) arrayList.add(i);
        long end1 = System.currentTimeMillis();

        long start2 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) linkedList.add(i);
        long end2 = System.currentTimeMillis();

        System.out.println("【尾部插入】");
        System.out.println("ArrayList: " + (end1 - start1) + " ms");
        System.out.println("LinkedList: " + (end2 - start2) + " ms\n");
    }

    // 中间插入 (每次插入到 1/2 位置)
    private static void testAddAtMiddle() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();

        long start1 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) arrayList.add(arrayList.size() / 2, i);
        long end1 = System.currentTimeMillis();

        long start2 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) linkedList.add(linkedList.size() / 2, i);
        long end2 = System.currentTimeMillis();

        System.out.println("【中间插入】");
        System.out.println("ArrayList: " + (end1 - start1) + " ms");
        System.out.println("LinkedList: " + (end2 - start2) + " ms\n");
    }

    // 头部插入
    private static void testAddAtHead() {
        List<Integer> arrayList = new ArrayList<>();
        List<Integer> linkedList = new LinkedList<>();

        long start1 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) arrayList.add(0, i);
        long end1 = System.currentTimeMillis();

        long start2 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; i++) linkedList.add(0, i);
        long end2 = System.currentTimeMillis();

        System.out.println("【头部插入】");
        System.out.println("ArrayList: " + (end1 - start1) + " ms");
        System.out.println("LinkedList: " + (end2 - start2) + " ms\n");
    }
}