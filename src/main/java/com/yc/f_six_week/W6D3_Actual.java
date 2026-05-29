public class W6D3_Actual {

    public static void main(String[] args) {


        /**
         *
         * 四、今日作业 2：AOP 和 IOC 的关系（口述版）
         *  一句话总结：IOC 是 AOP 的基础，AOP 是 IOC 的扩展应用。
         *  详细解释：
         *  IOC 是 AOP 的基础
         *  AOP 的底层实现依赖 Spring 的 IOC 容器管理 Bean，只有被 Spring 管理的 Bean 才能被代理，实现切面增强。
         *  如果没有 IOC 容器，AOP 无法自动创建代理对象、注入目标 Bean。
         *  AOP 是 IOC 的扩展
         *  IOC 解决了对象的创建和依赖注入问题；AOP 解决了通用逻辑（日志、事务、权限）的横向复用问题。
         *  AOP 利用 IOC 的特性，将切面逻辑织入到目标 Bean 中，而不需要修改业务代码。
         *  两者结合的典型场景
         *  Spring 事务管理（@Transactional）：IOC 管理事务管理器，AOP 通过代理织入事务开启 / 提交 / 回滚逻辑。
         *  日志、权限校验：IOC 管理切面 Bean，AOP 在方法执行前后织入日志 / 权限逻辑。
         *
         */
    }
}
