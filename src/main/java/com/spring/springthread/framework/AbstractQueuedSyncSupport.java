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
@SuppressWarnings("all")
public abstract class AbstractQueuedSyncSupport extends AbstractOwnedSynchronizer {
    /**
     * 同步状态
     */
    private volatile int state;
    /**
     * 队列头节点
     */
    private volatile QueueNode head;
    /**
     * 队列尾节点
     */
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

    /**
     * 原子更新同步状态
     *
     * @param expect 预期的状态值
     * @param update 待更新的新状态值
     * @return 如果状态值成功更新，则返回true；否则返回false
     */
    protected final boolean compareAndSetState(int expect, int update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, Integer> stateAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, Integer.class, "state");
        return stateAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }

    /**
     * CAS 更改尾节点
     * @param expect 预期节点
     * @param update 更新节点
     * @return 如果更新成功，则返回true；否则返回false
     */
    protected final boolean casTail(QueueNode expect, QueueNode update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, QueueNode.class, "tail");
        return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }

    /**
     * CAS 更改头节点
     * @param expect 预期节点
     * @param update 更新节点
     * @return 如果更新成功，则返回true；否则返回false
     */
    protected final boolean casHead(QueueNode expect, QueueNode update) {
        AtomicReferenceFieldUpdater<AbstractQueuedSyncSupport, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(AbstractQueuedSyncSupport.class, QueueNode.class, "head");
        return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
    }


    /**
     * 初始化头节点，如果头节点为空，则初始化一个空节点，否则返回尾节点
     * @return 尾节点
     */
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

    /**
     * 入队
     * @param node 节点
     */
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

    /**
     * 判断节点是否在队列中
     * @param node 节点
     * @return true：在队列中；false：不在队列中
     */
    final boolean isEnqueued(QueueNode node) {
        // question：为什么从尾部开始遍历？
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
            // 节点前驱为空，则说明当前节点为头节点，尝试占有锁 或者 node 为空，先尝试插队获取锁，否则排队阻塞
            Integer updated = tryOccupyAndUpdateState(node, arg, shared, first, pred, interrupted, currentThread);
            if (updated != null) {
                return updated;
            }

            QueueNode t;
            if ((t = tail) == null) { // 队列为空，则初始化队列头节点
                tryInitalizeQueue(arg, shared);
            } else if (node == null) { // 当前线程的节点为空，则初始化当前线程节点
                tryInitalizeCurrentNode(node, arg, shared);
            } else if (pred == null) { // 当前线程还没有入队，则链接前驱节点进行入队操作
                tryLinkAndCasTail(node, currentThread, t);
            } else if (first && spin != 0) {
                --spin;
                Thread.onSpinWait();
            } else if (node.status == 0) {
                node.setStatusRelaxed(WAITING);
            } else {
                long nanos;
                spin = postSpins = (byte) ((postSpins << 1) | 1);
                if (!timed)
                    LockSupport.park(this);
                else if ((nanos = timeout - System.nanoTime()) > 0L)
                    LockSupport.parkNanos(this, nanos);
                else
                    break;
                node.clearStatus();
                if ((interrupted |= Thread.interrupted()) && interruptible)
                    break;
            }
        }
        return cancelAcquire(node, interrupted, interruptible);
    }

    /**
     * 初始化队列
     * @apiNote 可能会出现初始化队列时 OOM异常，出现OOM时同步自旋获取锁直到获取成功
     * @param arg arg参数是修改的状态值
     * @param shared 是否是共享锁
     */
    private void tryInitalizeQueue(int arg, boolean shared) {
        if (tryInitQueueHeadNode() == null) {
            acquireOOME(shared, arg);
        }
    }

    /**
     * 尝试初始化当前线程节点
     * @apiNote 可能会出现OOM异常，出现OOM时同步自旋获取锁直到获取成功
     * @param node 节点
     * @param arg arg参数是修改的状态值
     * @param shared 是否是共享锁
     */
    private void tryInitalizeCurrentNode(QueueNode node, int arg, boolean shared) {
        try {
            node = shared ? new SharedNode() : new NoSharedNode();
        } catch (OutOfMemoryError error) {
            acquireOOME(shared, arg);
        }
    }

    /**
     * 尝试链接并CAS尾部节点
     * @apiNote 即 将 node 挂载在队列的尾部，挂载失败时恢复 node前驱等于null的状态
     * @param node 节点
     * @param currentThread 当前线程
     * @param t 尾部节点
     */
    private void tryLinkAndCasTail(QueueNode node, Thread currentThread, QueueNode t) {
        // 打包线程
        node.waiter = currentThread;
        // 将尾部节点设置为当前节点的前驱（还需要将尾部节点的后继设置为当前线程节点，才能实现两个节点的链接）
        node.setPrevRelaxed(t);
        // 尝试交换队列同步器中的尾部节点
        if (casTail(t, node)) {
            // 交换成功则挂载成功
            t.next = node;
        } else {
            // 挂载失败，当前节点未能成功入队，取消挂载
            node.setPrevRelaxed(null);
        }
    }

    /**
     * 尝试占有锁
     * @apiNote 当 当前节点是第一个有效节点 或 当前线程还没有入队进入阻塞时 尝试插队获取锁
     * @param node 当前线程节点
     * @param arg arg参数是修改的状态值
     * @param shared 是否是共享锁
     * @param first 是否是第一个有效节点
     * @param pred 当前节点的前驱节点
     * @param interrupted 是否响应中断
     * @param currentThread 当前线程
     * @return 获取锁的结果 0：获取锁失败，1：获取锁成功，null：不满足条件，跳过尝试获取锁的操作
     */
    private Integer tryOccupyAndUpdateState(QueueNode node, int arg, boolean shared, boolean first, QueueNode pred, boolean interrupted, Thread currentThread) {
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
                if (first) { // 获取锁成功，如果是队头有效节点就解除该节点（队头是null的空节点，它的下一个节点才是有效的排队线程节点）
                    node.prev = null;
                    pred.next = null;
                    head = node;
                    node.waiter = null;
                    if (shared) {
                        signalNextQueueNodeWhenShared(node);
                    }
                    if (interrupted) {
                        currentThread.isInterrupted();
                    }
                }
                return 1;
            }
        }
        return null;
    }

    /**
     * 无法创建节点时，内存不足时
     *
     * @param shared 是否是共享锁
     * @param arg arg参数是修改的状态值
     * @return 获取锁是否成功
     */
    private int acquireOOME(boolean shared, int arg) {
        // 自旋获取锁
        for (Long timeout = 0L; ; ) {
            if (tryAcquire(arg)) {
                return 1;
            }
            LockSupport.parkNanos(timeout);
            // 2^30 次自旋等待，不断增加等待时间 或者 尝试获取锁成功
            if (timeout < 1L << 30) {
                timeout = timeout << 1;
            }
        }
    }

    /**
     * 尝试占有锁
     * @param arg arg参数是修改的状态值 一般传递 1 ，当条件节点使用时可能会传递大于1的值用来控制条件
     * @return 是否占有锁
     */
    protected boolean tryAcquire(int arg) {
        throw new UnsupportedOperationException();
    }


    private int cancelAcquire(QueueNode node, boolean interrupted, boolean interruptible) {
        return 0;
    }

    private void cleanQueue() {

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

        /**
         * cas设置next节点
         * @param expect 期待值
         * @param update 更新值
         * @return 是否设置成功
         */
        final boolean casNext(QueueNode expect, QueueNode update) {
            AtomicReferenceFieldUpdater<QueueNode, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(QueueNode.class, QueueNode.class, "next");
            return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
        }

        /**
         * cas设置prev节点
         * @param expect 期待值
         * @param update 更新值
         * @return 是否设置成功
         */
        final boolean casPrev(QueueNode expect, QueueNode update) {
            AtomicReferenceFieldUpdater<QueueNode, QueueNode> nodeAtomicReferenceFieldUpdater = AtomicReferenceFieldUpdater.newUpdater(QueueNode.class, QueueNode.class, "prev");
            return nodeAtomicReferenceFieldUpdater.compareAndSet(this, expect, update);
        }

        /**
         * 设置prev节点，不保证原子性
         * @param p 节点
         */
        final void setPrevRelaxed(QueueNode p) {
            this.prev = p;
        }

        /**
         * 设置状态，不保证原子性
         * @param s 状态值
         */
        final void setStatusRelaxed(int s) {
            this.status = s;
        }

        /**
         * 清除状态
         */
        final void clearStatus() {
            this.status = 0;
        }

        /**
         * 获取并设置状态
         * @param s 状态值
         */
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

        /**
         * 阻塞当前线程，直到被唤醒
         * @return 是否被唤醒
         * @throws InterruptedException 线程被中断
         */
        @Override
        public boolean block() throws InterruptedException {
            // 状态大于1且线程没有被中断
            while (!isReleasable()) {
                // 阻塞当前线程
                LockSupport.park();
            }
            return true;
        }

        /**
         * 是否被释放
         * @return 是否被释放
         */
        @Override
        public boolean isReleasable() {
            return status <= 1 || java.lang.Thread.currentThread().isInterrupted();
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
