import com.yc.common.PECSTest;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static com.yc.common.PECSTest.addNumbers;
import static com.yc.common.PECSTest.printList;

public class day5Actual {


    public static void main(String[] args) throws InvocationTargetException, IllegalAccessException, NoSuchMethodException {
        // 写代码验证类型擦除：用反射往 List<String> 里塞一个 Integer，观察编译期和运行期差异
        // 1. 编译期：只能加 String
        List<String> list = new ArrayList<>();
        list.add("Java");

        // 2. 用反射绕过编译检查，添加 Integer
        Method addMethod = List.class.getMethod("add", Object.class);
        addMethod.invoke(list, 666); // 成功加入 Integer 类型！

        // 3. 输出结果
        System.out.println(list); // [Java, 666]
        System.out.println("list.get(0) 类型：" + list.get(0).getClass().getName());
        System.out.println("list.get(1) 类型：" + list.get(1).getClass().getName());
     // 结论（类型擦除本质）
        //编译期：泛型检查严格，List<String> 只能加字符串
        //运行期：泛型被擦掉，List<String> 变成原始类型 List（等同于 List<Object>）
        //所以反射可以插入任意类型，不会报错

        // 写一个泛型方法，用 PECS 原则设计参数
        // [    PECS 极简释义:
        //      生产者用extends，消费者用super
        //  1. 生产者 ? extends T
        //   往外读取数据，只出不进
        //   只能获取元素，不能添加元素
        //  2. 消费者 ? super T
        //   往里存入数据，只进不出
        //   可以添加元素，取出只能当作 Object
        //  速记口诀
        //   取上限 extends，存下限 super]

        printList(List.of(1, 2, 3)); // 生产者

        // 测试消费者
        List<Object> list = new ArrayList<>();
        addNumbers(list);
        System.out.println(list); // [10, 20]
    }
}