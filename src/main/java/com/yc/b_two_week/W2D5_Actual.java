import com.yc.z_common.LRUCacheByLinkedHashMap;

public class W2D5_Actual {

    // 手写一个简化版 LRU 缓存（用 LinkedHashMap 或 HashMap+双向链表实现）
    public static void main(String[] args) {
        // 缓存容量为3
        LRUCacheByLinkedHashMap<String, Integer> cache = new LRUCacheByLinkedHashMap<>(3);

        cache.put("A", 1);
        cache.put("B", 2);
        cache.put("C", 3);
        System.out.println("初始存入3个元素：" + cache); // [A, B, C]

        // 访问A，A变为最近使用
        cache.get("A");
        System.out.println("访问A后：" + cache); // [B, C, A]

        // 新增D，超出容量，淘汰最久未使用的B
        cache.put("D", 4);
        System.out.println("新增D，淘汰B：" + cache); // [C, A, D]
    }
}