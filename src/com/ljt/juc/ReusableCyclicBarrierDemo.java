package com.ljt.juc;

import java.util.concurrent.CyclicBarrier;

public class ReusableCyclicBarrierDemo {
    public static void main(String[] args) {
        // 创建一个 CyclicBarrier，指定 3 个线程参与，屏障动作为打印所有线程到达的信息
        CyclicBarrier barrier = new CyclicBarrier(3, () -> {
            System.out.println("All threads have arrived at the barrier. Proceeding to next phase.");
        });

        // 创建并启动线程
        for (int i = 0; i < 3; i++) {
            new Thread(() -> {
                try {
                    // 第一个屏障点
                    System.out.println(Thread.currentThread().getName() + " is waiting at the first barrier");
                    barrier.await(); // 等待其他线程到达
                    System.out.println(Thread.currentThread().getName() + " has passed the first barrier");

                    // 第二个屏障点
                    System.out.println(Thread.currentThread().getName() + " is waiting at the second barrier");
                    barrier.await(); // 等待其他线程到达
                    System.out.println(Thread.currentThread().getName() + " has passed the second barrier");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }
}