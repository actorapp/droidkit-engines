package com.droidkit.engine.list.adapter;

import android.database.sqlite.SQLiteDatabase;

import com.droidkit.engine.list.ListEngineClassConnector;
import com.droidkit.engine.sqlite.BinarySerializator;


public class SingleListSingleTableDataAdapter<V> extends FewListsSingleTableDataAdapter {

    public SingleListSingleTableDataAdapter(SQLiteDatabase database,
                                            String listEngineName,
                                            boolean ascSorting,
                                            BinarySerializator<V> binarySerializator,
                                            ListEngineClassConnector<V> classConnector) {
        super(database, 0, listEngineName, ascSorting, binarySerializator, classConnector);
    }

}
