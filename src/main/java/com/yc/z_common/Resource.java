package com.yc.z_common;

public class Resource implements AutoCloseable {
    private String name;

    public void day4Actual(String name) {
        this.name = name;
        System.out.println(name + "：创建成功");
    }

    // 必须实现的关闭方法
    @Override
    public void close() throws Exception {
        System.out.println(name + "：自动关闭（释放资源）");
    }

    // 模拟业务方法
    public void doSomething() {
        System.out.println(name + "：执行业务逻辑");
    }

    public Resource(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}