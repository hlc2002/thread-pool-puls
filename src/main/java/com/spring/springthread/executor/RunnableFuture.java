package com.spring.springthread.executor;

import java.util.concurrent.Future;

/**
 * @author spring
 * @since 2024/9/14 16:42:41
 * @apiNote
 * @version 1.0
 */
public interface RunnableFuture<V> extends Runnable, Future<V> {

}
