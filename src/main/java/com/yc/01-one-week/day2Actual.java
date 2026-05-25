public class day2Actual {

    // 重载和重写的区别（从编译期 vs 运行期、参数、返回值、访问修饰符 4 个维度对比）?
    // 对比维度	 重载 (Overload)	                重写 (Override)
    // 执行时期	 编译期绑定	                    运行期绑定
    // 参数列表	 参数个数 / 类型 / 顺序必须不同	    参数列表必须完全一致
    // 返回值	 可随意修改	                    返回值协变，不能随意改动
    // 访问修饰符	 权限无限制	                    权限只能放宽，不能缩小


    // 父类
    class Animal {
        public void shout() {
            System.out.println("动物发出叫声");
        }
    }

    // 子类1
    class Dog extends Animal {
        @Override
        public void shout() {
            System.out.println("小狗汪汪叫");
        }
    }

    // 子类2
    class Cat extends Animal {
        @Override
        public void shout() {
            System.out.println("小猫喵喵叫");
        }
    }

    public void main(String[] args) {
        //  手写一个展示多态的例子：父类引用分别指向不同子类对象，观察方法调用结果
        // 父类引用指向子类对象
        Animal animal1 = new Dog();
        Animal animal2 = new Cat();
        // 运行时调用子类重写方法
        animal1.shout();
        animal2.shout();

        //  设计一个接口 + 抽象类的综合示例，体现各自优势
        Vehicle car = new Car("奔驰");
        Vehicle bike = new Bike("永久");
        car.showBrand();
        car.run();
        car.stop();
        bike.showBrand();
        bike.run();
        bike.stop();
    }

    // 接口：定义行为规范
    interface Drive {
        void run();
    }

    // 抽象类：抽取公共属性与通用逻辑
    abstract class Vehicle implements Drive {
        protected String brand;

        public Vehicle(String brand) {
            this.brand = brand;
        }

        // 通用公有方法
        public void showBrand() {
            System.out.println("车辆品牌：" + brand);
        }

        // 抽象方法，子类必须实现
        public abstract void stop();
    }

    // 具体子类
    class Car extends Vehicle {
        public Car(String brand) {
            super(brand);
        }

        @Override
        public void run() {
            System.out.println(brand + " 公路行驶");
        }

        @Override
        public void stop() {
            System.out.println(brand + " 刹车停车");
        }
    }

    class Bike extends Vehicle {
        public Bike(String brand) {
            super(brand);
        }

        @Override
        public void run() {
            System.out.println(brand + " 骑行前进");
        }

        @Override
        public void stop() {
            System.out.println(brand + " 脚撑驻车");
        }
    }
}