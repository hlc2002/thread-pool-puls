package com.spring.springthread.pool;

import com.spring.springthread.reject.RejectedHandler;
import com.spring.springthread.executor.ScheduledExecuteSupport;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;


/**
 * @author spring
 * @version 1.0
 * @apiNote 可调度的线程池执行支持器
 * @since 2024/9/30 13:29:30
 * <p>
 *
 * </P>
 */
public class ScheduledThreadPoolExecuteSupport extends ThreadPoolExecuteSupport implements ScheduledExecuteSupport {


    private volatile boolean executeDelayTaskAfterShutdown = true; // 关闭线程池后执行未执行的延迟任务
    volatile boolean removeOnCancel; // 取消后是否移除任务

    private static final AtomicLong versionNumer = new AtomicLong();

    @Override
    public void schedule(Runnable command, long delay, TimeUnit unit) {

    }

    @Override
    public <V> V schedule(Callable<V> command, long delay, TimeUnit unit) {
        return null;
    }

    @Override
    public void execute(Runnable command) {

    }



    public ScheduledThreadPoolExecuteSupport(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                             TimeUnit unit, BlockingQueue<Runnable> taskQueue) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, taskQueue);
    }

    public ScheduledThreadPoolExecuteSupport(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                                             TimeUnit unit, BlockingQueue<Runnable> taskQueue, RejectedHandler rejectedHandler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, taskQueue, rejectedHandler);
    }

    public static void main(String[] args) {
        main0();
    }

    public static void main0() {
        ScheduledThreadPoolExecuteSupport scheduledThreadPoolExecuteSupport = new ScheduledThreadPoolExecuteSupport(2, 3, 10, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>());
        scheduledThreadPoolExecuteSupport.execute(() -> {
            System.out.println("hello world ! " + 1);
        });
        scheduledThreadPoolExecuteSupport.shutdown();
    }
}
