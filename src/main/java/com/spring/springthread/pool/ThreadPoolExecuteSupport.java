package com.spring.springthread.pool;

import com.spring.springthread.executor.DefaultRejectedHandler;
import com.spring.springthread.executor.RejectedHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author spring
 * @since 2024/9/14 16:18:38
 * @apiNote
 * @version 1.0
 */
public class ThreadPoolExecuteSupport extends AbstractExecuteSupport {
    /**
     * 在运行的线程数量
     */
    private final AtomicInteger ctl = new AtomicInteger(ctlOf(RUNNING, 0)); // 默认为 RUNNING 状态

    private boolean compareAndIncrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect + 1); // 比较并交换 需要重试直到成功
    }

    private boolean compareAndDecrementWorkerCount(int expect) {
        return ctl.compareAndSet(expect, expect - 1);
    }

    private void decrementWorkerCount() {
        ctl.addAndGet(-1); // 减一
    }

    /**
     * 32 - 3 = 29
     */
    private static final int COUNT_BITS = Integer.SIZE - 3;
    /**
     * 0001 1111 1111 1111   1111 1111 1111 1111
     */
    private static final int COUNT_MASK = (1 << COUNT_BITS) - 1;
    // 我们会发现高三位代表线程池状态 低29位代表线程数量
    // 111 运行 000 关闭 001 停止 010 整理 011 销毁
    /**
     * 1110 0000 0000 0000   0000 0000 0000 0000
     * 0000 0000 0000 0000   0000 0000 0000 0011
     */
    private static final int RUNNING = -1 << COUNT_BITS;
    /**
     * 0000 0000 0000 0000   0000 0000 0000 0000
     */
    private static final int SHUTDOWN = 0;
    /**
     * 0001 0000 0000 0000   0000 0000 0000 0000
     */
    private static final int STOP = 1 << COUNT_BITS;
    /**
     * 0100 0000 0000 0000   0000 0000 0000 0000
     */
    private static final int TIDYING = 2 << COUNT_BITS;
    /**
     * 0110 0000 0000 0000   0000 0000 0000 0000
     */
    private static final int TERMINATED = 3 << COUNT_BITS;

    private static int runStateOf(int c) {
        return c & ~COUNT_MASK;
    }

    private static int workerCountOf(int c) {
        return c & COUNT_MASK;
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    private LinkedBlockingQueue<Worker> workerQueue;
    private final HashSet<Worker> workers = new HashSet<>();
    private LinkedBlockingQueue<Runnable> taskQueue;

    private volatile RejectedHandler handler;
    private volatile DefaultRejectedHandler defaultRejectedHandler;

    final void rejected(Runnable command) {
        if (handler == null) {
            defaultRejectedHandler.rejected(command, this);
            return;
        }
        handler.rejected(command, this);
    }

    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private volatile int largestPoolSize;

    private final ReentrantLock mainLock = new ReentrantLock();

    @Override
    public <T> T submit(Callable<T> command) {
        return null;
    }


    @Override
    public void run() {

    }

    @Override
    public void execute(Runnable command) {

    }

    final void tryTerminate() {

    }

    /**
     * 核心线程池允许执行者
     */
    @Data
    @EqualsAndHashCode(callSuper = true)
    @SuppressWarnings("all")
    private final class Worker extends AbstractQueuedSynchronizer
            implements Runnable {

        final Thread thread;
        Runnable firstTask;
        volatile long completedTasks;

        Worker(Runnable firstTask) {
            setState(-1); // 初始化中断态 -1 当此值修改为 0 意味着线程运行
            this.firstTask = firstTask;
            thread = new Thread(this);
        }

        @Override
        public void run() {
            firstTask.run();
        }

        @Override
        protected boolean tryRelease(int arg) {
            return getState() == 0;
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        public boolean tryAcquire() {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        public boolean tryRelease() {
            if (compareAndSetState(1, 0)) {
                setExclusiveOwnerThread(null);
                return true;
            }
            return false;
        }

        public void lock() {
            acquire(1);
        }

        public void unlock() {
            release(1);
        }

        public boolean isLocked() {
            return isHeldExclusively();
        }

        void interruptIfStarted() {
            Thread t;
            if (getState() >= 0 && (t = thread) != null && !t.isInterrupted()) {
                try {
                    t.interrupt();
                } catch (SecurityException ignore) {
                }
            }
        }

    }

    private boolean addWorker(Runnable firstTask, boolean core) {
        loop:
        for (int state = ctl.get(); ; ) {
            if (state >= STOP) // 线程池已停止 直接拒绝添加核心 worker
                return false;
            while (true) {
                // 核心或非核心线程数 过载，即拒绝直接添加 worker （这里是先插队的思路）
                if (workerCountOf(state) >= ((core ? corePoolSize : maximumPoolSize) & COUNT_MASK)) {
                    return false;
                }
                if (compareAndIncrementWorkerCount(state)) // 核心线程数增加成功 插队成功 跳出大循环体（比较与交换的思维）
                    break loop;
                state = ctl.get(); // 添加失败继续自旋重试 直到成功
                if (state >= SHUTDOWN) // 线程池已停止，重新进入大循环体
                    continue loop;
                // 如果线程池未停止，该工作线程 自旋 的被添加进原子整数中。
            }
        }

        boolean addWorker = false; // 初始化添加成功标识
        boolean workerStart = false; // 初始化启动线程标识

        Worker worker = null; // 初始化工作线程
        try {
            worker = new Worker(firstTask);
            final Thread waitAddThread = worker.thread;

            if (waitAddThread != null) {
                final ReentrantLock lock = this.mainLock;
                lock.lock(); // 按住线程池内的共享可重入锁 按不住的阻塞
                try {
                    int state = ctl.get();
                    if (state >= RUNNING || (state >= STOP && firstTask == null)) {
                        if (waitAddThread.getState() != Thread.State.NEW)
                            throw new IllegalThreadStateException("线程状态异常：待运行的线程状态不是 新建！");
                    }
                    workers.add(worker);
                    addWorker = true;
                    int size = workers.size();
                    if (size > largestPoolSize)
                        largestPoolSize = size;
                } finally {
                    lock.unlock();
                }
                if (addWorker) {
                    waitAddThread.start(); // 启动线程 启用VM底层的 SharedThreadContainer 来管理线程
                    workerStart = true;
                }
            }
        } finally {
            if (!workerStart) { // 启动失败 则释放线程池内的资源
                final ReentrantLock mainLock = this.mainLock;
                mainLock.lock();
                try {
                    if (worker != null)
                        workers.remove(worker);
                    decrementWorkerCount(); // 释放线程池内的资源
                    tryTerminate(); // 尝试终止线程池
                } finally {
                    mainLock.unlock();
                }
            }
        }
        return workerStart;
    }
}
