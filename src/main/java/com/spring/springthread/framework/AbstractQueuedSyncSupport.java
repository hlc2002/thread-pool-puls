package com.spring.springthread.framework;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;

/**
 * @author spring
 * @version 1.0
 * @apiNote
 * @since 2024/10/3 16:06:39
 */
public abstract class AbstractQueuedSyncSupport extends AbstractOwnedSynchronizer {
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
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, Integer> stateAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, Integer.class, "state");
        return stateAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }

    @SuppressWarnings("all")
    protected final boolean casTail(QueueNode expect, QueueNode update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, QueueNode.class, "tail");
        return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }

    @SuppressWarnings("all")
    protected final boolean casHead(QueueNode expect, QueueNode update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, QueueNode.class, "head");
        return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }

    private QueueNode tryInitQueueHeadNode() {
        for (QueueNode h = null, t; ; ) {
            if ((t = tail) != null) {
                return t;
            } else if (head != null) {
                // 尾等于null，头不等于null，线程自旋等待（意味着有节点在尝试占有锁）
                Thread.onSpinWait();
            } else {
                if (h == null) {
                    try {
                        h = new NoSharedNode();
                    } catch (OutOfMemoryError error) {
                        return null;
                    }
                }
                // CAS 设置头节点 使用在循环中是为了保证自旋的设置成功，失败了则是多线程数据变化需要重试逻辑
                if (casHead(null, h)) {
                    // 设置尾节点
                    return tail = h;
                }
            }
        }
    }


    static class QueueNode {
        volatile QueueNode next;
        volatile QueueNode prev;
        volatile int status;
        Thread waiter;

        public QueueNode() {
        }

        public QueueNode(int status) {
            this.status = status;
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

    public static void main(String[] args) {
        QueueNode node = new QueueNode();
        node.setPrevRelaxed(new QueueNode(1));
        System.out.println(node.prev.status + "");
        // 测试修改效果
        node.casPrev(node.prev, new QueueNode(2));
        System.out.println(node.prev.status + "");
    }
}
