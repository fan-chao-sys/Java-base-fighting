package com.yc.Xcommon;

public class Student implements Comparable<Student> {
    private String name;
    private int age;

    public Student(String name, int age) {
        this.name = name;
        this.age = age;
    }

    // 2. 内部比较器：按【年龄 升序】排序
    @Override
    public int compareTo(Student other) {
        System.out.println("=== 内部 Comparable 被调用 ===");
        return this.age - other.age;
    }

    // getter
    public int getAge() {
        return age;
    }

    public String getName() {
        return name;
    }
}