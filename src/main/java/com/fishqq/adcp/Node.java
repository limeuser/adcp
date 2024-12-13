package com.fishqq.adcp;

final class Node {
    Node next;
    Node prev;
    ProxyConnection item;

    Node() {
        this(null);
    }

    Node(ProxyConnection item) {
        this.item = item;
    }

    void move(Node toNode) {
        this.remove();
        this.insertAfter(toNode);
    }

    void remove() {
        if (this.next != null) {
            this.next.prev = this.prev;
        }
        if (this.prev != null) {
            this.prev.next = this.next;
        }
    }

    void insertAfter(Node to) {
        Node n = to.next;
        to.next = this;
        this.prev = to;
        this.next = n;
        if (n != null) {
            n.prev = this;
        }
    }
}
