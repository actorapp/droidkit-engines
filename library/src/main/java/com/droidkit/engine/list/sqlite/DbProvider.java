package com.droidkit.engine.list.sqlite;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.droidkit.core.Logger;

import java.util.concurrent.LinkedBlockingQueue;

public class DbProvider {

    public static final String DB_NAME = "SECRET_DB";

    private static LinkedBlockingQueue<DatabaseCallback> callbacks = new LinkedBlockingQueue<DatabaseCallback>();

    private static volatile SQLiteDatabase db;

    private static volatile boolean dbOpeningInProgress = false;

    public synchronized static void getDatabase(final Context context, final DatabaseCallback callback) {
        if (db != null && callback != null) {
            callback.onDatabaseOpened(db);
        } else {
            if (callback != null) {
                callbacks.add(callback);
            }
            if (!dbOpeningInProgress) {
                dbOpeningInProgress = true;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            DbHelper helper = new DbHelper(context.getApplicationContext(), DB_NAME, null);
                            db = helper.getWritableDatabase();

                            DatabaseCallback c;
                            while ((c = callbacks.poll()) != null) {
                                c.onDatabaseOpened(db);
                            }
                        } catch (Exception e) {
                            Logger.e("Can't open SQLite", e);
                        }
                        dbOpeningInProgress = false;
                    }
                }).start();
            }
        }
    }

    public static interface DatabaseCallback {
        void onDatabaseOpened(SQLiteDatabase session);
    }

}
