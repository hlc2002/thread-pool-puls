package com.spring.springthread.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @author spring
 * @version 1.0
 * @apiNote
 * @since 2024/9/30 13:37:06
 */
public interface ScheduledExecuteSupport extends ExecuteSupport {
    void schedule(Runnable command, long delay, TimeUnit unit);

    <V> V schedule(Callable<V> command, long delay, TimeUnit unit);
}
