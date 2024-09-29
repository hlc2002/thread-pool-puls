package com.spring.springthread.pool;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author spring
 * @since 2024/9/14 16:44:29
 * @apiNote
 * @version 1.0
 */
public class FutureTask<V> implements RunnableFuture<V>{
    private final Runnable runnable;
    private final V result;

    public FutureTask(Runnable runnable, V result){
        this.runnable = runnable;
        this.result = result;
    }
    @Override
    public void run() {

    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
