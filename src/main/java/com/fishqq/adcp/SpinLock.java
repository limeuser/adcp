package com.fishqq.adcp;

import java.util.concurrent.atomic.AtomicBoolean;

public class SpinLock {
    private final AtomicBoolean state = new AtomicBoolean(false);

    public void lock() {
        while (true) {
            while (state.get()) {
            }

            if (!state.getAndSet(true)) {
                return;
            }
        }
    }

    public void unlock() {
        state.set(false);
    }
}
