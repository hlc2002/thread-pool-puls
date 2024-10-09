package com.spring.springthread.framework;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
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

    // CAS 更改尾节点
    @SuppressWarnings("all")
    protected final boolean casTail(QueueNode expect, QueueNode update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, QueueNode.class, "tail");
        return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }

    // CAS 更改头节点
    @SuppressWarnings("all")
    protected final boolean casHead(QueueNode expect, QueueNode update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, QueueNode.class, "head");
        return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }

    // 初始化头节点（仅当队列为空时初始化空节点，否则直接返回尾节点）
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

    // 新节点入队
    final void enqueue(QueueNode node) {
        if (node != null && node.waiter != null) {
            boolean unpark = false;
            for (QueueNode t; ; ) {
                // 如果队列为空 且 初始化节点仍然为空，说明堆空间不足，直接唤醒线程
                if ((t = tail) == null && (t = tryInitQueueHeadNode()) == null) {
                    unpark = true;
                    break;
                }
                // 将当前节点挂载在原先尾节点的后面，如果原先的尾部节点状态小于0，则唤醒它的线程
                node.setPrevRelaxed(t);
                if (casTail(t, node)) {
                    t.next = node;
                    if (t.status < 0) {
                        unpark = true;
                    }
                    break;
                }
            }
            if (unpark) {
                LockSupport.unpark(node.waiter);
            }
        }
    }

    // 判断节点是否在队列中
    final boolean isEnqueued(QueueNode node) {
        for (QueueNode t = tail; t != null; t = t.prev) {
            if (t == node) {
                return true;
            }
        }
        return false;
    }

    /**
     * 唤醒下一个节点
     * @param node 节点
     */
    private static void signalNextQueueNode(QueueNode node) {
        if (node != null && node.next != null && node.next.status != 0) {
            node.next.getAndReSetStatus(WAITING);
            LockSupport.unpark(node.next.waiter);
        }
    }

    /**
     * 唤醒下一个共享节点
     * @param node 节点
     */
    private static void signalNextQueueNodeWhenShared(QueueNode node) {
        if (node != null && node.next != null && node.next instanceof SharedNode && node.next.status != 0) {
            node.next.getAndReSetStatus(WAITING);
            LockSupport.unpark(node.next.waiter);
        }
    }

    /**
     * 获取锁
     * @param node 节点
     * @param arg arg参数是修改的状态值 一般传递 1 ，当条件节点使用时可能会传递大于1的值用来控制条件
     * @param shared 是否是共享锁
     * @param interruptible 是否可以响应中断
     * @param timed 是否控制自旋时间
     * @param timeout 自旋时间
     * @return 锁的状态
     */
    @SuppressWarnings("all")
    final int acquire(QueueNode node, int arg, boolean shared, boolean interruptible, boolean timed, Long timeout) {
        Thread currentThread = Thread.currentThread();
        byte spin = 0, postSpins = 0;
        boolean interrupted = false, first = false;
        // 节点前驱
        QueueNode pred = null;
        while (true) {
            if (!first && (pred = node == null ? null : node.prev) != null && !(first = head == pred)) {
                if (pred.status < 0) {
                    // todo 前驱的状态异常，取消队列中异常的节点
                    continue;
                } else if (pred.prev == null) {
                    // 前驱节点等待被唤醒执行，则自旋等待
                    Thread.onSpinWait();
                    continue;
                }
            }
            // 节点前驱为空，则说明当前节点为头节点，则直接尝试占有锁
            if (first || pred == null) {
                boolean locked = false;
                try {
                    if (shared) {
                        // todo 共享锁的实现
                    } else {
                        locked = tryAcquire(arg);
                    }
                } catch (Throwable e) {
                    // todo 取消获取锁的动作
                    throw e;
                }
                if (locked) {
                    if(first){
                        node.prev = null;
                        pred.next = null;
                        head = node;
                        node.waiter = null;
                        if (shared){
                            signalNextQueueNodeWhenShared(node);
                        }
                        if(interrupted){
                            currentThread.isInterrupted();
                        }
                    }
                    return 1;
                }
            }
        }
    }

    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
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

        final void getAndReSetStatus(int s) {
            AtomicIntegerFieldUpdater<QueueNode> integerFieldUpdater = AtomicIntegerFieldUpdater.newUpdater(QueueNode.class, "status");
            // 按位取反再与当前的状态进行与操作
            integerFieldUpdater.getAndSet(this, status & (~s));
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
