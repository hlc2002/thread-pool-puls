package com.spring.springthread.executor;

/**
 * @author spring
 * @since 2024/9/29 16:50:27
 * @apiNote
 * @version 1.0
 */
public interface RejectedHandler {
    void rejected(Runnable task, ExecuteSupport executor);
}
