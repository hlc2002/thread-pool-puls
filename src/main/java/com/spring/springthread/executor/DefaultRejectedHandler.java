package com.spring.springthread.executor;

import com.spring.springthread.pool.ExecuteSupport;

import java.util.concurrent.RejectedExecutionException;

/**
 * @author spring
 * @since 2024/9/29 19:55:32
 * @apiNote
 * @version 1.0
 */
public class DefaultRejectedHandler implements RejectedHandler {
    @Override
    public void rejected(Runnable task, ExecuteSupport executor) {
        if (task == null)
            throw new NullPointerException("task is null !");
        if (executor.isShutdown())
            throw new RejectedExecutionException("executor is shutdown !");
        throw new RejectedExecutionException("task is rejected !");
    }
}
