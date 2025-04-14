package com.ljt.juc;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class CyclicBarrierDemo {
    public static void main(String[] args) throws BrokenBarrierException, InterruptedException {

        CyclicBarrier cyclicBarrier = new CyclicBarrier(7, () -> System.out.println("*****召唤神龙"));
        // 七个参与者都到达屏障后才继续执行
        for (int i = 1; i <= 7; i++) {
            final int tempInt = i;
            new Thread(
                    () -> {
                        System.out.println(Thread.currentThread().getName() +
                                "\t 收集到第" + tempInt + "颗龙珠");
                        try {
                            // 让线程等待
                            cyclicBarrier.await();
                        } catch (InterruptedException | BrokenBarrierException e) {
                            throw new RuntimeException(e);
                        }

                    }
                    , String.valueOf(i)).start();
        }
    }
}
