package com.fishqq.adcp;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Pool<E> {
    private final Node idleHead;
    private final Node activeHead;
    private final Node emptyHead;

    private final ReentrantLock idleAndEmptyLock = new ReentrantLock();
    private final Condition noIdleOrEmptyNodeCondition = idleAndEmptyLock.newCondition();

    private final SpinLock activeLock = new SpinLock();

    private int idleCount;
    private int activeCount;

    public final class Node {
        Node next;
        Node prev;
        E item;
    }

    public enum NodeType {
        IDLE,
        EMPTY;
    }

    public Pool(int capacity) {
        this.idleHead = new Node();
        this.activeHead = new Node();
        this.emptyHead = new Node();

        List<Node> nodes = new ArrayList<>(capacity);
        for (int i = 0; i < capacity; i++) {
            nodes.add(new Node());
        }

        this.emptyHead.next = nodes.get(0);
        Node prev = this.emptyHead;

        for (int i = 0; i < capacity - 1; i++) {
            Node current = nodes.get(i);
            current.next = nodes.get(i + 1);
            current.prev = prev;
            prev = current;
        }

        nodes.get(nodes.size() - 1).prev = prev;
    }

    public int idleCount() {
        return idleCount;
    }

    public int activeCount() {
        return activeCount;
    }

    public void recycleIdleItems(Consumer<E> recycleHandler, Predicate<E> needRecycle, Supplier<Boolean> needStop) {
        idleAndEmptyLock.lock();

        try {
            Node node = idleHead.next;

            while (node != null) {
                if (needStop.get()) {
                    break;
                }

                if (needRecycle.test(node.item)) {
                    recycleHandler.accept(node.item);
                    Node next = node.next;
                    destroyIdle(node);
                    node = next;
                } else {
                    node = node.next;
                }
            }
        } finally {
            idleAndEmptyLock.unlock();
        }
    }

    private void destroyIdle(Node idle) {
        --idleCount;
        move(idle, emptyHead);
    }

    private boolean noIdleOrEmptyNode() {
        return idleHead.next == null && emptyHead.next == null;
    }

    public List<E> listActiveItems() {
        List<E> items = new ArrayList<>(activeCount);

        activeLock.lock();

        try {
            Node node = activeHead.next;
            while (node != null) {
                items.add(node.item);
                node = node.next;
            }

            return items;
        } finally {
            activeLock.unlock();
        }
    }

    public void destroyAll(Consumer<E> handler) {
        idleAndEmptyLock.lock();
        destroyAll(idleHead.next, handler);
        idleCount = 0;
        noIdleOrEmptyNodeCondition.signalAll();
        idleAndEmptyLock.unlock();

        activeLock.lock();
        destroyAll(activeHead.next, handler);
        activeCount = 0;
        activeLock.unlock();
    }

    private void destroyAll(Node first, Consumer<E> handler) {
        if (first != null) {
            Node last = destroyAllReturnLast(first, handler);
            Node firstEmpty = emptyHead.next;
            emptyHead.next = first;
            first.prev = emptyHead;
            last.next = firstEmpty;
            if (firstEmpty != null) {
                firstEmpty.prev = last;
            }
        }
    }

    private Node destroyAllReturnLast(Node node, Consumer<E> handler) {
        Node last = null;

        while (node != null) {
            handler.accept(node.item);
            last = node;
            node = node.next;
        }

        return last;
    }

    public void destroyActive(Node active, Consumer<E> handler) {
        idleAndEmptyLock.lock();

        try {
            boolean empty = noIdleOrEmptyNode();

            activeLock.lock();
            E item = active.item;
            --activeCount;
            move(active, emptyHead);
            handler.accept(item);
            activeLock.unlock();

            if (empty) {
                signalNotEmpty();
            }
        } finally {
            idleAndEmptyLock.unlock();
        }
    }

    private void signalNotEmpty() {
        idleAndEmptyLock.lock();
        try {
            noIdleOrEmptyNodeCondition.signal();
        } finally {
            idleAndEmptyLock.unlock();
        }
    }

    public Pair<NodeType, Node> tryTakeIdleOrEmptyNode(long timeoutMs) {
        long nanos = TimeUnit.MICROSECONDS.toNanos(timeoutMs);

        idleAndEmptyLock.lock();

        while (noIdleOrEmptyNode()) {
            if (nanos <= 0) {
                idleAndEmptyLock.unlock();
                return null;
            }

            try {
                nanos = noIdleOrEmptyNodeCondition.awaitNanos(nanos);
            } catch (Throwable e) {
                // just return and retry
                idleAndEmptyLock.unlock();
                return null;
            }
        }

        Node idle = idleHead.next;

        if (idle != null) {
            activeLock.lock();
            --idleCount;
            ++activeCount;
            move(idle, activeHead);
            activeLock.unlock();
            idleAndEmptyLock.unlock();
            return new Pair<>(NodeType.IDLE, idle);
        } else {
            Node emptyNode = remove(emptyHead.next);
            idleAndEmptyLock.unlock();
            return new Pair<>(NodeType.EMPTY, emptyNode);
        }
    }

    // move active node to idle list
    public void giveBack(Node active) {
        idleAndEmptyLock.lock();

        try {
            boolean empty = noIdleOrEmptyNode();

            activeLock.lock();
            ++idleCount;
            --activeCount;
            move(active, idleHead);
            activeLock.unlock();

            if (empty) {
                signalNotEmpty();
            }
        } finally {
            idleAndEmptyLock.unlock();
        }
    }

    public void pushToActive(Node node) {
        activeLock.lock();
        insertAfter(node, activeHead);
        ++activeCount;
        activeLock.unlock();
    }

    private void move(Node movingNode, Node toNode) {
        remove(movingNode);
        insertAfter(movingNode, toNode);
    }

    private Node remove(Node node) {
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        if (node.prev != null) {
            node.prev.next = node.next;
        }
        return node;
    }

    private void insertAfter(Node node, Node to) {
        Node n = to.next;
        to.next = node;
        node.prev = to;
        node.next = n;
        if (n != null) {
            n.prev = node;
        }
    }
}
