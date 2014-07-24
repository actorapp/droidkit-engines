package com.droidkit.engine.list.adapter;

import android.database.sqlite.SQLiteDatabase;

import com.droidkit.engine.list.ListEngineItemSerializator;


public class SingleListSingleTableDataAdapter<V> extends FewListsSingleTableDataAdapter {

    public SingleListSingleTableDataAdapter(SQLiteDatabase database, String listEngineName,
                                            boolean ascSorting, ListEngineItemSerializator<V> listEngineItemSerializator) {
        super(database, 0, listEngineName, ascSorting, listEngineItemSerializator);
    }

}
