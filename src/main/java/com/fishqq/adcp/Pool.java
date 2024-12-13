package com.fishqq.adcp;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class Pool {
    private final int max;
    private final Lock fullLock = new ReentrantLock();
    private final Condition fullCondition = fullLock.newCondition();

    private final AtomicInteger idleCount;
    private final AtomicInteger activeCount;
    private final AtomicInteger totalCount;

    private final ReadWriteLock poolListLock = new ReentrantReadWriteLock();
    private final List<ThreadLocalPool> localPools = new LinkedList<>();
    private final ThreadLocal<ThreadLocalPool> poolThreadLocal = ThreadLocal.withInitial(() -> {
        ThreadLocalPool pool = new ThreadLocalPool();
        poolListLock.writeLock().lock();
        localPools.add(pool);
        poolListLock.writeLock().unlock();
        return pool;
    });

    Pool(int max) {
        this.max = max;
        this.idleCount = new AtomicInteger(0);
        this.activeCount = new AtomicInteger(0);
        this.totalCount = new AtomicInteger(0);
    }

    int idleCount() {
        return idleCount.get();
    }

    int activeCount() {
        return activeCount.get();
    }

    Node borrow() {
        ThreadLocalPool currentThreadPool = poolThreadLocal.get();

        if (idleCount.get() > 0) {
            Node local = currentThreadPool.tryTake();
            if (local != null) {
                idleCount.decrementAndGet();
                activeCount.incrementAndGet();
                return local;
            }
        }

        if (idleCount.get() > 0) {
            poolListLock.readLock().lock();

            for (ThreadLocalPool pool : localPools) {
                if (pool != currentThreadPool) {
                    Node stolen = pool.trySteal();
                    if (stolen != null) {
                        poolListLock.readLock().unlock();
                        idleCount.decrementAndGet();
                        activeCount.incrementAndGet();
                        currentThreadPool.addStolenConnection(stolen);
                        return stolen;
                    }
                }
            }

            poolListLock.readLock().unlock();
        }

        return null;
    }

    boolean tryAddConnection(long timeoutMs, Consumer<Long> waitHandler) {
        long nanosTimeout = timeoutMs * 1000;

        fullLock.lock();

        try {
            while (totalCount.get() == max) {
                try {
                    long remaining = fullCondition.awaitNanos(nanosTimeout);
                    if (remaining <= 0) {
                        // timeout
                        waitHandler.accept(nanosTimeout - remaining);
                        return false;
                    }

                    waitHandler.accept(nanosTimeout - remaining);
                    nanosTimeout = remaining;
                } catch (InterruptedException e) {
                    return false;
                }
            }

            totalCount.incrementAndGet();

            return true;
        } finally {
            fullLock.unlock();
        }
    }

    void failedAddConnection() {
        decrementTotalCountAndNotify();
    }

    void giveBack() {
        idleCount.incrementAndGet();
        activeCount.decrementAndGet();
    }

    void recycleIdleItems(Consumer<Connection> recycleHandler,
                          Predicate<ProxyConnection> needRecycle,
                          Supplier<Boolean> needStop) {
        poolListLock.readLock().lock();

        try {
            for (ThreadLocalPool pool : localPools) {
                if (needStop.get()) {
                    break;
                }

                pool.recycle(conn -> {
                    recycleHandler.accept(conn);
                    idleCount.decrementAndGet();
                    decrementTotalCountAndNotify();
                }, needRecycle, needStop);
            }
        } finally {
            poolListLock.readLock().unlock();
        }
    }

    void destroyAll(Consumer<ProxyConnection> handler) {
        poolListLock.writeLock().lock();
        for (ThreadLocalPool pool : localPools) {
            pool.destroy(handler);
        }
        localPools.clear();
        poolListLock.writeLock().unlock();

        idleCount.set(0);
        activeCount.set(0);
        totalCount.set(0);

        fullCondition.signalAll();
    }

    void destroyActive(Node active) {
        ThreadLocalPool pool = poolThreadLocal.get();
        pool.removeConnection(active);
        activeCount.decrementAndGet();
        decrementTotalCountAndNotify();
    }

    private void decrementTotalCountAndNotify() {
        if (totalCount.decrementAndGet() == max - 1) {
            fullLock.lock();
            try {
                fullCondition.signal();
            } finally {
                fullLock.unlock();
            }
        }
    }

    void addNewConnection(ProxyConnection connection) {
        ThreadLocalPool pool = poolThreadLocal.get();
        pool.addNewConnection(new Node(connection));
        activeCount.incrementAndGet();
    }
}
