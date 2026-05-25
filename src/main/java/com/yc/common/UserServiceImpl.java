package com.yc.common;

// 2. 接口实现类（被代理对象）
public class UserServiceImpl implements UserService {
    @Override
    public void addUser(String name) {
        System.out.println("【业务】添加用户：" + name);
    }

    @Override
    public void deleteUser(int id) {
        System.out.println("【业务】删除用户ID：" + id);
    }
}
