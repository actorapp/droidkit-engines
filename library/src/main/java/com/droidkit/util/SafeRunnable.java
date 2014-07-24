package com.droidkit.util;

import com.droidkit.core.Logger;

public abstract class SafeRunnable implements Runnable {

    @Override
    public void run() {
        try {
            runSafely();
        } catch (final Exception t) {
            Logger.e("SafeRunnable", "Exception in SafeRunnable", t);
        }
    }

    public abstract void runSafely();
}
