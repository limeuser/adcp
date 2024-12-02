package com.fishqq.adcp;

import java.util.concurrent.atomic.AtomicBoolean;

public class SpinLock {
    private volatile Thread thread;
    private final AtomicBoolean state = new AtomicBoolean(false);

    public void lock() {
        while (true) {
            while (state.get()) {
            }

            if (!state.getAndSet(true)) {
                thread = Thread.currentThread();
                return;
            }
        }
    }

    public void unlock() {
        state.set(false);
        thread = null;
    }
}
