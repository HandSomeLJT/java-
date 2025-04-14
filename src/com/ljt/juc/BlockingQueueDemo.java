package com.ljt.juc;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

public class BlockingQueueDemo {
    public static void main(String[] args) throws InterruptedException {
        ArrayBlockingQueue<String> arrayBlockingQueue = new ArrayBlockingQueue<>(3);
        addAndRemove(arrayBlockingQueue);
        offerAndPoll(arrayBlockingQueue);
        putAndTake(arrayBlockingQueue);
        offerOutOfTime(arrayBlockingQueue);
    }


    public static void addAndRemove(BlockingQueue<String> blockingQueue) {
        System.out.println("----------------addAndRemove()----------------");
        System.out.println(blockingQueue.add("a"));
        System.out.println(blockingQueue.add("b"));
        System.out.println(blockingQueue.add("c"));
        // Queue full
        //System.out.println(blockingQueue.add("d"));

        System.out.println(blockingQueue.remove());
        System.out.println(blockingQueue.remove());
        System.out.println(blockingQueue.remove());
        //error
        //System.out.println(blockingQueue.remove());
    }

    public static void offerAndPoll(BlockingQueue<String> blockingQueue) {
        System.out.println("----------------offerAndPoll()----------------");
        System.out.println(blockingQueue.offer("a"));
        System.out.println(blockingQueue.offer("b"));
        System.out.println(blockingQueue.offer("c"));
        System.out.println(blockingQueue.offer("d"));
        // peek 查看队首
        System.out.println(blockingQueue.peek());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
        System.out.println(blockingQueue.poll());
    }

    public static void putAndTake(BlockingQueue<String> blockingQueue) throws InterruptedException {
        System.out.println("----------------putAndTake()----------------");
        blockingQueue.put("a");
        blockingQueue.put("b");
        blockingQueue.put("c");
        // 由于超过队列的长度 添加d将会阻塞 在这里阻塞
        // blockingQueue.put("d");
        System.out.println(blockingQueue.take());
        System.out.println(blockingQueue.take());
        System.out.println(blockingQueue.take());
        // 由于队列为空 这里将会阻塞
        // System.out.println(blockingQueue.take());
    }


    public static void offerOutOfTime(BlockingQueue<String> blockingQueue) throws InterruptedException {
        System.out.println("----------------offerOutOfTime()----------------");
        System.out.println(blockingQueue.offer("a", 3L, TimeUnit.SECONDS));
        System.out.println(blockingQueue.offer("b", 3L, TimeUnit.SECONDS));
        System.out.println(blockingQueue.offer("c", 3L, TimeUnit.SECONDS));
        // 由于队列满了 这里将会阻塞3秒后返回false
        System.out.println(blockingQueue.offer("d", 3L, TimeUnit.SECONDS));
    }

}
