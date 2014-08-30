package com.droidkit.engine.list.sqlite;

import android.database.sqlite.SQLiteDatabase;

import com.droidkit.engine.list.DataAdapter;


public class SQLiteStorageAdapter<V> extends SQLiteMultipleStorageAdapter {

    public SQLiteStorageAdapter(SQLiteDatabase database,
                                String listEngineName,
                                boolean ascSorting,
                                DataAdapter<V> classConnector) {
        super(database, 0, listEngineName, ascSorting, classConnector);
    }

    public SQLiteStorageAdapter(SQLiteDatabase database,
                                String listEngineName,
                                DataAdapter<V> classConnector) {
        super(database, 0, listEngineName, false, classConnector);
    }

}
