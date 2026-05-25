package com.yc.common;

// 自定义类
public class User {
    private String name;
    private int id;

    public User(String name, int id) {
        this.name = name;
        this.id = id;
    }

    // ========================
    // 【关键】重写 equals 和 hashCode
    // ========================
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id; // 根据 id 判断是否相等
    }

    // private 方法
    private void sayHello(String msg) {
        System.out.println("Hello, " + name + "! " + msg);
    }

    @Override
    public int hashCode() {
        return id; // 用 id 计算哈希值
    }
}