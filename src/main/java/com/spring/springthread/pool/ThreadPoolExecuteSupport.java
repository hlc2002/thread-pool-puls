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
 * @version 1.0
 * @apiNote ThreadPoolExecuteSupport
 * @since 2024/9/14 16:18:38
 * <p>
 * 线程池化执行支持器
 * 特点：
 * 1、原子整数记录线程池状态 与 线程数量；
 * 2、worker作为工作线程主体不断向runnableQueue拉取任务执行，这个过程是并行的；
 * 3、默认提交任务时尝试直接向worker投递任务，如果失败则尝试向阻塞队列投递任务，投递失败则抛出异常；
 * 4、如果提交任务失败，则尝试向拒绝策略投递任务，如果失败则抛出异常；
 * 5、线程池主锁 保证线程安全，通过 CAS 保证线程池状态的修改；
 * 6、线程池状态的修改通过 CAS 保证原子性；
 * 7、worker 线程通过 CAS 获取锁，保证worker同时只执行一个任务；
 * 待优化：
 * 1、线程创建应当配置工厂类实现；
 * 2、统一管理线程状态的容器：底层使用并发容器 SharedThreadContainer 实现管理；
 * 3、部分功能接口的完善；
 * 使用方式：
 * new ThreadPoolExecuteSupport(5, 10, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>())
 * .execute(() -> System.out.println("hello world"));
 * </P>
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

    private final HashSet<Worker> workers = new HashSet<>(); // 线程池工作线程集合
    private BlockingQueue<Runnable> runnableQueue; // 任务队列

    private volatile RejectedHandler handler; // 拒绝策略

    final void rejected(Runnable command) {
        handler.rejected(command, this);
    }

    private volatile int corePoolSize; // 线程池核心线程数量
    private volatile int maximumPoolSize; // 线程池最大线程数量 不含阻塞队列
    private volatile int largestPoolSize; // 线程池最大线程数量 含阻塞队列

    private volatile boolean allowCoreThreadTimeOut; // 是否允许核心任务超时
    private volatile long keepAliveTime; // 任务保持活跃时间

    private final ReentrantLock mainLock = new ReentrantLock(); // 线程池可重入锁
    private static final RuntimePermission shutdownPerm = new RuntimePermission("modifyThread"); // 线程池权限

    @Override
    public void run() {

    }

    /**
     * 执行任务
     *
     * @param command 任务
     */
    @Override
    public void execute(Runnable command) {
        if (command == null)
            throw new NullPointerException("command is null !");
        System.out.println("worker count : " + workerCountOf(ctl.get()) + ", core pool size : " + corePoolSize);
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

    /**
     * 尝试销毁线程池
     */
    final void tryTerminate() {

    }

    /**
     * 关闭线程池
     */
    public void shutdown() {
        final ReentrantLock lock = this.mainLock;
        lock.lock();
        try {
            checkPermission(); // 检查权限
            for (; ; ) {
                // 循环判断线程池状态 并尝试修改状态 直到 线程池状态为 SHUTDOWN 或者修改状态为 SHUTDOWN 成功
                if (ctl.get() >= SHUTDOWN || ctl.compareAndSet(ctl.get(), ctlOf(SHUTDOWN, workerCountOf(ctl.get()))))
                    break;
            }
            interruptWorkers(); // 中断所有工作线程
            onShutdown(); // 钩子函数 在线程池关闭时执行某些操作
        } finally {
            lock.unlock();
        }
    }

    /**
     * 中断所有工作线程
     */
    private void interruptWorkers() {
        final ReentrantLock lock = this.mainLock; // 重入锁
        lock.lock();
        try {
            for (Worker worker : workers) {
                try {
                    Thread thread = worker.thread;
                    // 线程未中断 且 尝试获取阻塞其他任务成功
                    if (!thread.isInterrupted() && worker.tryAcquire(1)) {
                        thread.interrupt(); // 中断线程
                    }
                } finally {
                    worker.unlock();
                }
            }
        } finally {
            lock.unlock();
        }
    }

    /**
     * 检查权限
     */
    private void checkPermission() {
        SecurityManager securityManager = System.getSecurityManager();
        if (securityManager != null) {
            securityManager.checkPermission(shutdownPerm);
            for (Worker worker : workers) {
                // 检查线程权限 允许修改 线程
                securityManager.checkAccess(worker.thread);
            }
        }
    }

    /**
     * 钩子函数 在线程池关闭时执行某些操作
     */
    void onShutdown() {
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
            setState(-1); // 初始化中断态 -1 当此值修改为 0 意味着其他任务可交给工作线程允许
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

        /**
         * 允许其他任务运行
         */
        public void unlock() {
            release(1);
        }

        /**
         * 其他任务可运行
         *
         * @param unused 重写标识
         * @return 是否成功
         */
        protected boolean tryRelease(int unused) {
            setExclusiveOwnerThread(null);
            setState(0);
            return true;
        }

        /**
         * 阻塞其他任务
         */
        public void lock() {
            acquire(1);
        }

        /**
         * 尝试将其他任务可进入运行态 改为 其他任务阻塞态
         * 这里需要重写 tryAcquire 方法
         *
         * @param unused 重写标识
         * @return 是否成功
         */
        protected boolean tryAcquire(int unused) {
            if (compareAndSetState(0, 1)) {
                setExclusiveOwnerThread(Thread.currentThread());
                return true;
            }
            return false;
        }

        /**
         * 其他任务是否处在阻塞态（已经有任务在执行，其他任务阻塞）
         *
         * @return
         */
        public boolean isLocked() {
            return isHeldExclusively();
        }

        /**
         * 中断
         */
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

    /**
     * 添加 worker
     *
     * @param firstTask 初始任务
     * @param core      是否核心线程 不是核心线程 就根据最大线程数来判断 添加
     * @return boolean 是否添加成功
     */
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

    /**
     * 工作线程的启动方法
     *
     * @param worker 新生的工作线程对象
     */
    final void runWorker(Worker worker) {
        Thread currentThread = Thread.currentThread();
        Runnable task = worker.firstTask;
        worker.firstTask = null; // 当firstTask等于空时，说明该worker已经进入工作状态，不为空时是说明该worker刚创建完毕
        worker.unlock(); // 允许添加新的task任务（第一次执行worker的run方法时 此代码的作用是将 state 置为 0 意思就是进入运行时 -1 是中断时）
        boolean completedAbruptly = true; // 允许中断
        try {
            while (task != null || (task = getTask()) != null) {
                worker.lock(); // 不允许添加新的task任务 此时会将 state 置为 1 意思就是 阻塞中（有任务在执行）
                if ((ctl.get() >= STOP || Thread.interrupted() && ctl.get() >= STOP) && !currentThread.isInterrupted()) {
                    currentThread.interrupt();
                }
                try {
                    beforeExecute(currentThread, task);
                    try {
                        task.run(); // 执行 runnable 任务
                        afterExecute(task, null);
                    } catch (Exception e) {
                        afterExecute(task, e);
                        throw e;
                    }
                } finally {
                    task = null;
                    worker.completedTasks++; // 记录工作线程 已经完成的任务数量
                    worker.unlock(); // 也允许添加新的task任务 将 state 置为 0
                }
            }
            completedAbruptly = false; // 不允许中断
        } finally {
            processWorkerExit(worker, completedAbruptly); // 释放工作线程的资源 意思就是退出此工作线程，在 runnableQueue中 已经拉取不到任务了
        }
    }

    /**
     * 工作线程拉取任务队列并执行的方法
     *
     * @return 任务对象
     */
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
                // 当工作线程数量已经大于核心线程数 或 允许线程超时 时 延时获取任务 否则直接获取runnable queue 中的头部任务
                Runnable runnable = timed ? runnableQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) : runnableQueue.take();
                if (runnable != null)
                    return runnable;
                timedOut = true;
            } catch (InterruptedException e) {
                timedOut = false;
            }
        }
    }

    /**
     * 工作线程执行任务前的钩子方法
     *
     * @param thread 工作线程对象
     * @param task   要执行的任务对象
     */
    protected void beforeExecute(Thread thread, Runnable task) {

    }

    /**
     * 工作线程执行任务后的钩子方法
     *
     * @param runnable  执行的任务对象
     * @param throwable 执行过程中抛出的异常
     */
    protected void afterExecute(Runnable runnable, Throwable throwable) {

    }

    /**
     * 工作线程退出时的钩子方法
     *
     * @param worker            工作线程对象
     * @param completedAbruptly 是否是突然异常退出
     */
    private void processWorkerExit(Worker worker, boolean completedAbruptly) {

    }

    /**
     * 移除任务
     *
     * @param runnable 任务对象
     * @return boolean 是否移除成功
     */
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
        threadPoolExecuteSupport.shutdown();
    }
}
