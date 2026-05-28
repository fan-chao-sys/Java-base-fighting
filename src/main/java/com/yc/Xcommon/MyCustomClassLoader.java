package com.yc.Xcommon;

import java.io.FileInputStream;
import java.io.IOException;

/**
 * 自定义类加载器：从指定目录加载 .class 文件
 */
public class MyCustomClassLoader extends ClassLoader {

    // 要加载的 class 文件所在目录
    private final String classPath;

    public MyCustomClassLoader(String classPath) {
        this.classPath = classPath;
    }

    /**
     * 重写核心方法：findClass
     */
    @Override
    protected Class<?> findClass(String className) throws ClassNotFoundException {
        try {
            // 1. 读取 class 文件字节码
            byte[] classData = getClassData(className);
            
            // 2. 调用 JDK 提供的 defineClass 将字节码转为 Class 对象
            return defineClass(className, classData, 0, classData.length);
        } catch (IOException e) {
            throw new ClassNotFoundException("无法加载类：" + className, e);
        }
    }

    /**
     * 从文件系统读取 .class 字节码
     */
    private byte[] getClassData(String className) throws IOException {
        // 把全类名转为文件路径：com.test.User → com/test/User.class
        String path = classPath 
                + "/" 
                + className.replace(".", "/") 
                + ".class";

        try (FileInputStream fis = new FileInputStream(path)) {
            byte[] buffer = new byte[fis.available()];
            fis.read(buffer);
            return buffer;
        }
    }

}