package com.spring.springthread.pool;

import java.util.concurrent.Callable;

/**
 * @author spring
 * @since 2024/9/14 16:18:09
 * @apiNote
 * @version 1.0
 */
public interface Execute {
    void execute(Runnable command);
}
