package com.spring.springthread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.concurrent.DefaultManagedAwareThreadFactory;

import java.util.concurrent.*;

public class SpringThreadApplication {
    final void run() {
        while (true) {
            System.out.println("hello world");
        }
    }

    public static void main(String[] args) {
        // 模拟worker
        Worker worker = new Worker(() -> System.out.println("hello word !"));
        Thread thread = worker.thread;
        thread.start();

        try {
            CountDownLatch countDownLatch = new CountDownLatch(1);
            ThreadPoolExecutor executor = new ThreadPoolExecutor(100, 100, 0L,
                    TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
            executor.submit(countDownLatch::countDown);
            countDownLatch.await();
            System.out.println("end");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static class Worker {
        Runnable firstTask;
        Thread thread;

        Worker(Runnable firstTask) {
            this.firstTask = firstTask;
            this.thread = new Thread(firstTask);
        }
    }
}
