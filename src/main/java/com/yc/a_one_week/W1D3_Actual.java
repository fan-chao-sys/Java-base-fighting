public class W1D3_Actual {


    public static void main(String[] args) {
        // 用 javap 反编译一段字符串拼接代码，观察编译器优化（+ 是否转为 StringBuilder）
         // 实验结果：
            // Java 编译器对 + 做了优化
            // 普通拼接 → 自动用 StringBuilder
            // 循环拼接 → 不会优化，必须手动写 StringBuilder

        // 手写代码验证 intern() 行为：new String("abc") == "abc" vs new String("abc").intern() == "abc"
        String s1 = new String("abc");
        String s2 = "abc";

        // 对比1
        System.out.println(s1 == s2); // fasle
        // 对比2
        System.out.println(s1.intern() == s2);// true  intern() 会把字符串存入常量池，返回常量池引用，和字面量地址一致，== 为 true


        // 画一张字符串常量池的内存示意图（栈 → 堆 → 常量池关系）
            //        栈内存
            //        ├─ s1 引用地址 →────┐
            //        ├─ s2 引用地址 →────┘
            //        └─ s3 引用地址 →─────────────┐
            //
            //        堆内存
            //        ├─ 字符串常量池
            //        │   └─ "abc" 唯一实例
            //        │
            //        └─ 普通对象区
            //            └─ new String("abc") 新建对象
            //                内部字符数据指向常量池"abc"

       // 整理笔记：3 种字符串拼接方式（String / StringBuilder / StringBuffer）的性能对比
        // 1. String + 少量拼接
        String s = "";
        for(int i=0;i<10000;i++){
            s += i;
        }

        // 2. StringBuilder  单线程循环拼接
        StringBuilder sb = new StringBuilder();
        for(int i=0;i<10000;i++){
            sb.append(i);
        }

        // 3. StringBuffer   多线程共享拼接
        StringBuffer sf = new StringBuffer();
        for(int i=0;i<10000;i++){
            sf.append(i);
        }

    }
}