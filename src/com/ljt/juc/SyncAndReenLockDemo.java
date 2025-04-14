package com.ljt.juc;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 多线程之问按顺序调用，实现A->B->C三个线程启动，
 * 要求如下:AA打印5次，BB打印10次，cc打印15次，
 * 紧接着，AA打印5次，BB打印10次，cc打印15次，
 * ...
 * 来10轮
 */
public class SyncAndReenLockDemo {
    public static void main(String[] args) {
        ShareResource shareResource = new ShareResource();

        // 启动线程AA，循环调用print5方法10次
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                shareResource.print5();
            }
        }, "AA").start();

        // 启动线程BB，循环调用print10方法10次
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                shareResource.print10();
            }
        }, "BB").start();

        // 启动线程CC，循环调用print15方法10次
        new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                shareResource.print15();
            }
        }, "CC").start();

    }

}

class ShareResource {
    private int number = 1;
    private final ReentrantLock lock = new ReentrantLock();
    Condition condition1 = lock.newCondition();
    Condition condition2 = lock.newCondition();
    Condition condition3 = lock.newCondition();

    public void print5() {
        // 加锁
        lock.lock();
        try {
            // 循环等待，直到number等于1
            while (number != 1) {
                condition1.await();
            }
            // 打印当前线程名称和1到5的数字
            for (int i = 0; i < 5; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + (i + 1));
            }
            // 将number设置为2
            number = 2;
            // 通知等待在condition2上的线程
            condition2.signal();
        } catch (InterruptedException e) {
            // 捕获中断异常，并抛出运行时异常
            throw new RuntimeException(e);
        } finally {
            // 解锁
            lock.unlock();
        }
    }


    public void print10() {
        lock.lock();
        try {
            while (number != 2) {
                condition2.await();
            }
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + (i + 1));
            }
            number = 3;
            condition3.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    public void print15() {
        lock.lock();
        try {
            while (number != 3) {
                condition3.await();
            }
            for (int i = 0; i < 10; i++) {
                System.out.println(Thread.currentThread().getName() + "\t" + (i + 1));
            }
            number = 1;
            condition1.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }
}
