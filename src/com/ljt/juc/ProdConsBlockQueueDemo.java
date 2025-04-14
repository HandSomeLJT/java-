package com.ljt.juc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ProdConsBlockQueueDemo {
    public static void main(String[] args) {
        MyResource myResource = new MyResource(new ArrayBlockingQueue<>(10));
        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "\t生产线程启动");
            try {
                myResource.myProd();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "prod").start();

        new Thread(() -> {
            System.out.println(Thread.currentThread().getName() + "\t消费线程启动");
            try {
                myResource.myCons();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "cons").start();

        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (Exception e) {
            e.printStackTrace();
        }

        System.out.println("5秒钟后，叫停");
        myResource.stop();
    }
}

class MyResource {
    private volatile boolean FLAG = true; //默认开启，进行生产+消费
    private AtomicInteger atomicInteger = new AtomicInteger();

    private BlockingQueue<String> blockingQueue;

    public MyResource(BlockingQueue<String> blockingQueue) {
        this.blockingQueue = blockingQueue;
    }

    public void myProd() throws Exception {
        String data;
        boolean retValue;
        // 循环条件：当FLAG为true时继续循环
        while (FLAG) {
            // 原子性地增加atomicInteger的值，并将其转换为字符串
            data = atomicInteger.incrementAndGet() + "";//++i
            // 尝试在阻塞队列中插入数据，等待时间为2秒
            retValue = blockingQueue.offer(data, 2L, TimeUnit.SECONDS);
            // 如果插入成功
            if (retValue) {
                // 打印当前线程名及插入成功的信息
                System.out.println(Thread.currentThread().getName() + "\t" + "插入队列" + data + "成功");
            } else {
                // 打印当前线程名及插入失败的信息
                System.out.println(Thread.currentThread().getName() + "\t" + "插入队列" + data + "失败");
            }
            // 线程休眠1秒
            TimeUnit.SECONDS.sleep(1);
        }
        // 打印当前线程名及停止生产的信息
        System.out.println(Thread.currentThread().getName() + "\tFLAG==false，停止生产");
    }


    public void myCons() throws Exception {
        // 定义一个字符串变量res
        String res;
        // 当FLAG为true时，进入循环
        while (FLAG) {
            // 从阻塞队列中尝试获取元素，超时时间为2秒
            res = blockingQueue.poll(2L, TimeUnit.SECONDS);
            // 如果res为null或者res为空字符串
            if (null == res || res.equalsIgnoreCase("")) {
                // 将FLAG设置为false
                FLAG = false;
                // 输出当前线程名称和提示信息
                System.out.println(Thread.currentThread().getName() + "\t超过2秒钟没有消费，退出消费");
                // 退出方法
                return;
            }
            // 输出当前线程名称和消费成功的信息
            System.out.println(Thread.currentThread().getName() + "\t消费队列" + res + "成功");
        }
    }


    public void stop() {
        this.FLAG = false;
    }
}
