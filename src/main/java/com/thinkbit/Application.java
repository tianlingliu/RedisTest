package com.thinkbit;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Application {


    public static ExecutorService executorService = null;

    public static volatile boolean isRun = false;


    static {
        executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);
    }


    public static void main(String[] args) {


        System.out.println("-------------单线程无锁");
        for (int i = 0; i < 2; i++) {
            singleThread();
        }


        System.out.println("----------------单线程加锁");
        for (int i = 0; i < 2; i++) {
            singleLockThread();
        }

        System.out.println("----------------多线程加锁");
        for (int i = 0; i < 2; i++) {
            moreLockThread();
        }

        executorService.shutdown();
        System.exit(0);

    }

    public static void singleThread() {
        RedisPoolUtil.initialPool();

        Jedis jedis = RedisPoolUtil.getConn();
        System.out.println("服务正在运行: " + jedis.ping());
        jedis.set("up", "0");


        long current = System.currentTimeMillis();
        System.out.println("服务运行开始时间: " + current);
        long stop = current + 1000 * 10;
        isRun = true;


        while (stop > System.currentTimeMillis()) {
            jedis.incr("up");

        }
        System.out.println("服务运行结束时间: " + System.currentTimeMillis());

        System.out.println("10秒递增数量：" + jedis.get("up"));
    }


    public static void singleLockThread() {

        RedisPoolUtil.initialPool();

        Jedis jedis = RedisPoolUtil.getConn();
        System.out.println("服务正在运行: " + jedis.ping());
        jedis.set("up", "0");


        long current = System.currentTimeMillis();
        System.out.println("服务运行开始时间: " + current);
        long stop = current + 1000 * 10;
        isRun = true;


        while (stop > System.currentTimeMillis()) {
            Transaction multi = jedis.multi();
            multi.incr("up");
            multi.lpush("upList", "test");
            multi.exec();
        }
        System.out.println("服务运行结束时间: " + System.currentTimeMillis());

        System.out.println("10秒递增数量：" + jedis.get("up"));
    }


    public static void moreLockThread() {

        RedisPoolUtil.initialPool();

        Jedis jedis = RedisPoolUtil.getConn();
        System.out.println("服务正在运行: " + jedis.ping());
        jedis.set("up", "0");
        RedisPoolUtil.closeConn();


        for (int i = 0; i < Runtime.getRuntime().availableProcessors() * 2; i++) {

            jedis.incr("up");
            executorService.execute(() -> {
                while (true) {
                    if (isRun) {
                        Jedis once = RedisPoolUtil.getConn();

                        Transaction multi = once.multi();
                        multi.incr("up");
                        multi.lpush("upList", "test");
                        multi.exec();
                        RedisPoolUtil.closeConn();
                    }
                }
            });
        }
        long current = System.currentTimeMillis();
        System.out.println("服务运行开始时间: " + current);
        long stop = current + 1000 * 10;
        isRun = true;


        while (stop > System.currentTimeMillis()) {

        }


        System.out.println("服务运行结束时间: " + System.currentTimeMillis());
        System.out.println("10秒递增数量：" + RedisPoolUtil.getConn().get("up"));

    }


}
