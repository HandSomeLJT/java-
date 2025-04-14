package com.ljt.juc;

import java.util.concurrent.locks.ReentrantLock;

import static com.ljt.juc.Phone.syncTest;

public class ReentrantLockDemo {
    public static void main(String[] args) throws InterruptedException {
        Phone phone = new Phone();
        syncTest(phone);
        Thread.sleep(1000);

        new Thread(phone, "A").start();
        new Thread(phone, "B").start();

        deadLockDemo();
    }

    private static void deadLockDemo() {
        ReentrantLock lock = new ReentrantLock();
        new Thread(()->{
            lock.lock();
            lock.lock();
            try {
                System.out.println(Thread.currentThread().getName() + "\t" + "lock()");
            }finally {
                lock.unlock();
            }
            System.out.println(Thread.currentThread().getName() + "\t" + "unlock()");
        }).start();
    }
}

class Phone implements Runnable {
    //Synchronized TEST

    public static void syncTest(Phone phone) {

        new Thread(() -> {
            try {
                phone.sendSMS();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "t1").start();

        new Thread(() -> {
            try {
                phone.sendSMS();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "t2").start();
    }

    public synchronized void sendSMS() {
        System.out.println(Thread.currentThread().getName() + "\t" + "sendSMS()");
        sendEmail();
    }

    public synchronized void sendEmail() {
        System.out.println(Thread.currentThread().getName() + "\t" + "sendEmail()");
    }

    @Override
    public void run() {
        get();
    }

    ReentrantLock lock = new ReentrantLock();

    public void get() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "\t" + "get()");
            set();
        } finally {
            lock.unlock();
        }
    }

    public void set() {
        lock.lock();
        try {
            System.out.println(Thread.currentThread().getName() + "\t" + "set()");
        } finally {
            lock.unlock();
        }
    }
}