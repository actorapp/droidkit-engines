package com.droidkit.engine.event;


public interface NotificationListener {
    void onNotification(int eventType, int eventId, Object[] eventArgs);
}
