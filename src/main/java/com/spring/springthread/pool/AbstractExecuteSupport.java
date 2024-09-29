package com.spring.springthread.pool;

import jakarta.annotation.Nullable;

import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

/**
 * @author spring
 * @since 2024/9/14 16:26:12
 * @apiNote
 * @version 1.0
 */
public abstract class AbstractExecuteSupport implements ExecuteSupport {


    public AbstractExecuteSupport() {
    }

    @Override
    public Future<?> submit(Runnable task) {
        if (task == null) {
            throw new NullPointerException("task is null !");
        }
        Future<?> future = new FutureTask<>(task, null);
        execute(task);
        return future;
    }


    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public void shutdown() {

    }

    @Override
    public void shutdownNow() {

    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

}
