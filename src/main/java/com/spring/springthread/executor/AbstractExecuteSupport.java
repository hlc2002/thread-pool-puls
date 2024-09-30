package com.spring.springthread.executor;

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
