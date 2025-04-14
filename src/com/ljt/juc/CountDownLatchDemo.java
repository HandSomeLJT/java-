package com.ljt.juc;

import java.util.Objects;
import java.util.concurrent.CountDownLatch;

public class CountDownLatchDemo {
    public static void main(String[] args) throws InterruptedException {
        country();
    }

    private static void country() throws InterruptedException {
        CountDownLatch countDownLatch = new CountDownLatch(6);
        for (int i = 1; i <= 6; i++) {
            new Thread(() -> {
                System.out.println(Thread.currentThread().getName() + "\t 国被灭");
                // 计数器减1 表示一个国家被灭
                countDownLatch.countDown();
            }, Objects.requireNonNull(CountryEnum.list(i)).getRetMsg()).start();
        }
        // 主线程等待6个国家被灭，才能继续执行
        countDownLatch.await();
        System.out.println(Thread.currentThread().getName() + "\t ******秦国一统华夏");
    }
}

enum CountryEnum {

    ONE(1, "齐"),

    TWO(2, "楚"),

    THREE(3, "燕"),

    FOUR(4, "韩"),

    FIVE(5, "赵"),

    SIX(6, "魏");

    private final Integer retCode;
    private final String retMsg;

    CountryEnum(Integer retCode, String retMsg) {
        this.retCode = retCode;
        this.retMsg = retMsg;
    }

    public Integer getRetCode() {
        return retCode;
    }
    public String getRetMsg() {
        return retMsg;
    }

    public static CountryEnum list(int index) {
        CountryEnum[] values = CountryEnum.values();
        for (CountryEnum value : values) {
            if (value.getRetCode() == index) {
                return value;
            }
        }
        return null;
    }
}