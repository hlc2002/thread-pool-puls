package com.spring.springthread;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.concurrent.DefaultManagedAwareThreadFactory;

import java.util.concurrent.*;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;

public class SpringThreadApplication {
    public static void main(String[] args) {
        SpringThreadApplication application = new SpringThreadApplication();
        application.main0();
        application.main1();
    }

    public void main0() {
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

    public void main1(){
        Worker worker = new Worker(() -> System.out.println("hello word !"));
    }

    public final class Worker extends AbstractQueuedSynchronizer {
        Runnable firstTask;
        Thread thread;

        Worker(Runnable firstTask) {
            this.firstTask = firstTask;
            this.thread = new Thread(firstTask);
        }
        public void lock() {
            acquire(0);
        }
        @Override
        public boolean tryAcquire(int ued) {
            if (compareAndSetState(0,1)){
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }
        public void unlock() {
            release(1);
        }
        @Override
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }
    }
}
