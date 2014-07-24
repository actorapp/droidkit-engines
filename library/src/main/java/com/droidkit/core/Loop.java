package com.droidkit.core;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

public class Loop extends Thread {
    public Handler handler;
    private Object handlerSyncObject;

    public Loop(String name) {
        setName(name);

        handlerSyncObject = new Object();
    }

    private void initializeHandler() {
        if (handler == null) {
            try {
                synchronized (handlerSyncObject) {
                    handlerSyncObject.wait();
                }
            } catch (Throwable t) {
                Logger.e(t);
            }
        }
    }

    public void sendMessage(Message msg, int delay) {
        initializeHandler();

        if (handler != null) {
            if (delay <= 0)
                handler.sendMessage(msg);
            else
                handler.sendMessageDelayed(msg, delay);
        }
    }

    public void sendRunnable(Runnable r, int delay) {
        initializeHandler();

        if (handler != null) {
            if (delay <= 0)
                handler.post(r);
            else
                handler.postDelayed(r, delay);
        }
    }

    @Override
    public void run() {
        Looper.prepare();

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                processMessage(msg);
            }
        };

        synchronized (handlerSyncObject) {
            handlerSyncObject.notify();
        }

        Looper.loop();
    }

    protected void processMessage(Message msg) {
        // Should be overridden
    }
}