package com.fishqq.adcp;

import java.sql.Connection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

final class ThreadLocalPool {
    private final SpinLock nodeLock;
    private final Node head;

    ThreadLocalPool() {
        this.nodeLock = new SpinLock();
        this.head = new Node(null);
    }

    void addNewConnection(Node node) {
        nodeLock.lock();
        node.insertAfter(head);
        nodeLock.unlock();
    }

    void addStolenConnection(Node node) {
        nodeLock.lock();
        node.insertAfter(head);
        nodeLock.unlock();
    }

    void removeConnection(Node node) {
        nodeLock.lock();
        node.remove();
        nodeLock.unlock();
    }

    Node tryTake() {
        nodeLock.lock();

        Node node = head.next;
        while (node != null) {
            if (node.item.tryUse()) {
                nodeLock.unlock();
                return node;
            }
            node = node.next;
        }

        nodeLock.unlock();

        return null;
    }

    Node trySteal() {
        nodeLock.lock();

        Node node = head.next;
        while (node != null) {
            if (node.item.tryUse()) {
                node.remove();
                nodeLock.unlock();
                return node;
            }
            node = node.next;
        }

        nodeLock.unlock();

        return null;
    }

    void recycle(Consumer<Connection> recycleHandler,
                 Predicate<ProxyConnection> needRecycle,
                 Supplier<Boolean> needStop) {
        nodeLock.lock();

        try {
            Node node = head.next;
            while (node != null) {
                if (needStop.get()) {
                    return;
                }

                if (node.item.isClosed() && needRecycle.test(node.item)) {
                    Connection raw = node.item.getRawConnection();

                    Node next = node.next;
                    node.remove();
                    node = next;

                    recycleHandler.accept(raw);
                } else {
                    node = node.next;
                }
            }
        } finally {
            nodeLock.unlock();
        }
    }

    void destroy(Consumer<ProxyConnection> handler) {
        nodeLock.lock();

        Node node = this.head.next;
        while (node != null) {
            handler.accept(node.item);
            node = node.next;
        }

        this.head.next = null;

        nodeLock.unlock();
    }
}
