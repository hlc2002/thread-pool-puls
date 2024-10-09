package com.spring.springthread.lock;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * @author spring
 * @version 1.0
 * @apiNote
 * @since 2024/10/3 16:06:39
 */
public abstract class AbstractQueuedSynchronizer extends AbstractOwnedSynchronizer {
    private volatile int state;
    private volatile QueueNode head;
    private volatile QueueNode tail;

    static final int WAITING = 1;
    static final int CONDITION_WAITING = 2;
    static final int CANCEL = 0X80000000;

    protected final int getState() {
        return state;
    }

    protected final void setState(int newState) {
        state = newState;
    }

    // CAS 更改state的值
    protected final boolean compareAndSetState(int expect, int update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSynchronizer, Integer> stateAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSynchronizer.class, Integer.class, "state");
        return stateAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }


    static class QueueNode {
        volatile QueueNode next;
        volatile QueueNode prev;
        volatile int status;
        Thread waiter;

        public QueueNode() {
        }

        final boolean casNext(QueueNode expect, QueueNode update) {
            AtomicReferenceFieldUpdater<QueueNode, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(QueueNode.class, QueueNode.class, "next");
            return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
        }

        final boolean casPrev(QueueNode expect, QueueNode update) {
            AtomicReferenceFieldUpdater<QueueNode, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(QueueNode.class, QueueNode.class, "prev");
            return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
        }

        final void setPrevRelaxed(QueueNode p) {
            this.prev = p;
        }

        final void setStatusRelaxed(int s) {
            this.status = s;
        }

        final void clearStatus() {
            this.status = 0;
        }
    }

    // 非共享节点
    static final class NoSharedNode extends QueueNode {
    }

    // 共享节点
    static final class SharedNode extends QueueNode {
    }

    // 条件节点
    static final class ConditionNode extends QueueNode implements ForkJoinPool.ManagedBlocker {
        ConditionNode nextWaiter;

        @Override
        public boolean block() throws InterruptedException {
            // 状态大于1且线程没有被中断
            while (!isReleasable()) {
                // 阻塞当前线程
                LockSupport.park();
            }
            return true;
        }

        @Override
        public boolean isReleasable() {
            return status <= 1 || Thread.currentThread().isInterrupted();
        }
    }
}
