package com.ljt.juc;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class CASDemo {
    public static void main(String[] args) {
        AtomicInteger atomicInteger = new AtomicInteger(5);
        System.out.println(atomicInteger.compareAndSet(5, 2019) + "\t current data : " + atomicInteger.get());
        //修改失败
        System.out.println(atomicInteger.compareAndSet(5, 1024) + "\t current data : " + atomicInteger.get());
        AtomicReferenceDemo();
    }

    public static void AtomicReferenceDemo() {
        User user1 = new User("Jack", 25);
        User user2 = new User("Lucy", 21);
        AtomicReference<User> atomicReference = new AtomicReference<>();
        atomicReference.set(user1);
        System.out.println(atomicReference.compareAndSet(user1, user2));
        System.out.println(atomicReference.compareAndSet(user1, user2));
    }
}

class User {

    private String name;
    private int age;

    public User(String name, int age) {
        this.name = name;
        this.age = age;
    }
}