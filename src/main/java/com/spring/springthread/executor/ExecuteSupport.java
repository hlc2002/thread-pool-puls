package com.spring.springthread.executor;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * @author spring
 * @since 2024/9/14 16:17:48
 * @apiNote
 * @version 1.0
 */
public interface ExecuteSupport extends Execute, Runnable, AutoCloseable {

    boolean isShutdown();

    boolean isTerminated();

    void shutdown();

    void shutdownNow();

    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

    @Override
    default void close() {

    }
}
