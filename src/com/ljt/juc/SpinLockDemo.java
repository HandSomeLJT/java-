package com.ljt.juc;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SpinLockDemo {

    AtomicReference<Thread> lock = new AtomicReference<>();

    public static void main(String[] args) {
        SpinLockDemo spinLock = new SpinLockDemo();
        new Thread(() -> {
            spinLock.lock();
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            spinLock.unlock();
        }, "t1").start();

        new Thread(() -> {
            spinLock.lock();
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            spinLock.unlock();
        }, "t2").start();
    }


    public void lock() {
        Thread thread = Thread.currentThread();
        System.out.println(Thread.currentThread().getName() + "\t" + "lock()");
        while (!lock.compareAndSet(null, thread)) {
            // 自旋
        }
    }

    public void unlock() {
        Thread thread = Thread.currentThread();
        System.out.println(Thread.currentThread().getName() + "\t" + "unlock()");
        lock.compareAndSet(thread, null);
    }

}
