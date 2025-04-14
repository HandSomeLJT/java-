package com.ljt.juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

class MyData {

    volatile int number = 0;
    AtomicInteger atomicInteger = new AtomicInteger();

    //此时number前面没有volatile 其他线程不能访问到最新的number值
    // synchronized可以保证原子性，但是效率低
    public synchronized void addPlusPlus() {
        number++;
    }

    public void setTo60() {
        this.number = 60;
    }

    public void addAtomic() {
        atomicInteger.getAndIncrement();
    }
}

//volatile可以保证可见性，及时通知其它线程主物理内存的值已被修改
public class VolatileDemo {
    public static void main(String[] args) {
        volatileDemo();
        atomicDemo();
    }

    private static void volatileDemo() {
        System.out.println("可见性测试");
        MyData myData = new MyData();//资源类
        //启动一个线程操作共享数据
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "\t come in");
            try {
                TimeUnit.SECONDS.sleep(3);
                //改变number的值 此处不加volatile main获取不到number的最新值
                myData.setTo60();
                System.out.println(Thread.currentThread().getName() + "\t update number value: " + myData.number);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "AAA").start();
        while (myData.number == 0) {
            //main线程持有共享数据的拷贝，一直为0
        }
        System.out.println(Thread.currentThread().getName() + "\t mission is over. main get number value: " + myData.number);
    }

    private static void atomicDemo() {
        System.out.println("原子性测试");
        MyData myData = new MyData();
        for (int i = 1; i <= 20; i++) {
            new Thread(() -> {
                for (int j = 0; j < 1000; j++) {
                    //不能保证原子性，可能出现并发问题 因此使用AtomicInteger
                    myData.addPlusPlus();
                    myData.addAtomic();
                }
            }, String.valueOf(i)).start();
        }
        while (Thread.activeCount() > 2) {
            Thread.yield();
        }
        System.out.println(Thread.currentThread().getName() + "\t int type finally number value: " + myData.number);
        System.out.println(Thread.currentThread().getName() + "\t AtomicInteger type finally number value: " + myData.atomicInteger);
    }
}
