package com.ljt.juc;

public class SingletonDemo {
    private SingletonDemo() {
        System.out.println(Thread.currentThread().getName() + "\t 我是构造方法");
    }

    //private static SingletonDemo instance;
    private volatile static SingletonDemo instance;
    //DCL模式 Double Check Lock 双端检索机制：在加锁前后都进行判断
    public static SingletonDemo getInstance() {
        if (instance == null) {
            synchronized (SingletonDemo.class) {
                if (instance == null) {
                    instance = new SingletonDemo();
                }
            }
        }
        return instance;
    }

    public static void main(String[] args) {
        for (int i = 1; i <= 1000; i++) {
            new Thread(() -> SingletonDemo.getInstance(), String.valueOf(i)).start();
        }
    }
}
