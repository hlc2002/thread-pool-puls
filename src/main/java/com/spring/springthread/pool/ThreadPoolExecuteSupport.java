package com.spring.springthread.pool;

import com.spring.springthread.executor.AbstractExecuteSupport;
import com.spring.springthread.executor.DefaultRejectedHandler;
import com.spring.springthread.executor.RejectedHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashSet;
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
        return c & ~COUNT_MASK; // ~ 是按位取反 即 ~COUNT_MASK = 1110 0000 0000 0000 0000 0000 0000 0000
    }

    private static int workerCountOf(int c) {
        return c & COUNT_MASK; // COUNT_MASK = 0001 1111 1111 1111 1111 1111 1111 1111
    }

    private static int ctlOf(int rs, int wc) {
        return rs | wc;
    }

    private final HashSet<Worker> workers = new HashSet<>();
    private BlockingQueue<Runnable> runnableQueue;

    private volatile RejectedHandler handler;

    final void rejected(Runnable command) {
        handler.rejected(command, this);
    }

    private volatile int corePoolSize;
    private volatile int maximumPoolSize;
    private volatile int largestPoolSize;

    private volatile boolean allowCoreThreadTimeOut;
    private volatile long keepAliveTime;

    private final ReentrantLock mainLock = new ReentrantLock();

    @Override
    public void run() {

    }

    @Override
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException("command is null !");
        if (workerCountOf(ctl.get()) < corePoolSize) {
            if (addWorker(command, true)) {
                return;
            }
        }
        if (ctl.get() < SHUTDOWN && runnableQueue.offer(command)) {
            if (ctl.get() >= SHUTDOWN && remove(command)) {
                rejected(command);
            } else if (workerCountOf(ctl.get()) == 0) {
                addWorker(null, false);
            }
        } else if (!addWorker(command, false)) {
            rejected(command);
        }
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
        private static final long serialVersionUID = 1L;

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
            runWorker(this);
        }

        @Override
        protected boolean isHeldExclusively() {
            return getState() != 0;
        }

        public void unlock() {
            release(1);
        }

        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }


        public void lock() {
            acquire(1);
        }

        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
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
                    System.out.println("SecurityException");
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
                    System.out.println("start thread " + waitAddThread.getName());
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

    final void runWorker(Worker worker) {
        Thread currentThread = Thread.currentThread();
        Runnable task = worker.firstTask;
        worker.firstTask = null;
        worker.unlock(); // 允许添加新的task任务（第一次执行worker的run方法时 此代码的作用是将 state 置为 0 意思就是运行时 -1 是中断时）
        boolean completedAbruptly = true; // 允许中断
        try {
            while (task != null || (task = getTask()) != null) {
                worker.lock(); // 不允许添加新的task任务
                if ((ctl.get() >= STOP || Thread.interrupted() && ctl.get() >= STOP) && !currentThread.isInterrupted()) {
                    currentThread.interrupt();
                }
                try {
                    beforeExecute(currentThread, task);
                    try {
                        task.run();
                        afterExecute(task, null);
                    } catch (Exception e) {
                        afterExecute(task, e);
                        throw e;
                    }
                } finally {
                    task = null;
                    worker.completedTasks++;
                    worker.unlock();
                }
            }
            completedAbruptly = false;
        } finally {
            processWorkerExit(worker, completedAbruptly);
        }
    }

    private Runnable getTask() {
        boolean timedOut = false; // 不允许超时
        for (; ; ) {
            if (ctl.get() >= SHUTDOWN && runnableQueue.isEmpty()) {
                decrementWorkerCount();
                return null;
            }
            int workerCount = workerCountOf(ctl.get());
            boolean timed = allowCoreThreadTimeOut || workerCount > corePoolSize;
            if ((workerCount > maximumPoolSize || (timedOut && allowCoreThreadTimeOut)) && (workerCount > 1 || runnableQueue.isEmpty())) {
                if (compareAndDecrementWorkerCount(ctl.get()))
                    return null;
                continue;
            }

            try {
                // 工作线程数量已经大于核心线程数 或 允许线程超时 时 延时获取任务 否则直接获取runnable queue 中的头部任务
                Runnable runnable = timed ? runnableQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : runnableQueue.take();
                if (runnable != null)
                    return runnable;
                timedOut = true;
            } catch (InterruptedException e) {
                timedOut = false;
            }
        }
    }

    protected void beforeExecute(Thread thread, Runnable task) {

    }

    protected void afterExecute(Runnable runnable, Throwable throwable) {

    }

    private void processWorkerExit(Worker worker, boolean completedAbruptly) {

    }

    public boolean remove(Runnable runnable) {
        boolean remove = runnableQueue.remove(runnable);
        tryTerminate();
        return remove;
    }

    public ThreadPoolExecuteSupport(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                    TimeUnit unit, BlockingQueue<Runnable> taskQueue) {
        this(corePoolSize, maximumPoolSize, keepAliveTime, unit, taskQueue, new DefaultRejectedHandler());
    }

    public ThreadPoolExecuteSupport(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                    TimeUnit unit, BlockingQueue<Runnable> taskQueue,
                                    RejectedHandler rejectedHandler) {
        this.corePoolSize = corePoolSize;
        this.maximumPoolSize = maximumPoolSize;
        this.keepAliveTime = unit.toNanos(keepAliveTime);
        this.runnableQueue = taskQueue;
        this.handler = rejectedHandler;
    }

    public static void main(String[] args) {
        main0();
    }

    public static void main0() {
        ThreadPoolExecuteSupport threadPoolExecuteSupport = new ThreadPoolExecuteSupport(2, 3, 10,
                TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
        threadPoolExecuteSupport.execute(() -> {
            System.out.println("hello world ! " + 1);
        });
        threadPoolExecuteSupport.execute(() -> {
            System.out.println("hello world ! " + 2);
        });
        threadPoolExecuteSupport.execute(() -> {
            System.out.println("hello world ! " + 3);
        });
        threadPoolExecuteSupport.execute(() -> {
            System.out.println("hello world ! " + 4);
        });
        threadPoolExecuteSupport.execute(() -> {
            System.out.println("hello world ! " + 5);
        });
    }
}
