package com.ljt.juc;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class CallableDemo {
    //实现Callable接口
    static class MyThread implements Callable<Integer> {
        @Override
        public Integer call() throws Exception {
            System.out.println("callable come in ...");
            return 1024;
        }
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        //创建FutureTask类，接受MyThread。
        FutureTask<Integer> futureTask = new FutureTask<>(new MyThread());
        //将FutureTask对象放到Thread类的构造器里面。
        new Thread(futureTask, "AA").start();
        int result01 = 100;
        //用FutureTask的get方法得到返回值。
        int result02 = futureTask.get();
        System.out.println("result=" + (result01 + result02));

    }
}