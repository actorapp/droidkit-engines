package com.droidkit.engine.list.adapter;

import android.database.sqlite.SQLiteDatabase;

import com.droidkit.engine.list.ListEngineClassConnector;
import com.droidkit.engine.list.sqlite.ListEngineItemDao;
import com.droidkit.engine.sqlite.BinarySerializator;

import java.util.ArrayList;

public class FewListsSingleTableDataAdapter<V> implements ListEngineDataAdapter {

    private final ListEngineItemDao dao;

    public FewListsSingleTableDataAdapter(SQLiteDatabase database,
                                          long listEngineId,
                                          String listEngineName,
                                          boolean ascSorting,
                                          BinarySerializator<V> binarySerializator,
                                          ListEngineClassConnector<V> classConnector) {
        dao = new ListEngineItemDao(listEngineName, listEngineId, database, ascSorting, binarySerializator, classConnector);
    }

    @Override
    public void insertSingle(Object item) {
        dao.insert(item);
    }

    @Override
    public void insertOrReplaceSingle(Object item) {
        dao.insertOrReplace(item);
    }

    @Override
    public void deleteSingle(long id) {
        dao.delete(id);
    }

    @Override
    public void insertBatch(ArrayList items) {
        dao.insertInTx(items);
    }

    @Override
    public void insertOrReplaceBatch(ArrayList items) {
        dao.insertOrReplaceInTx(items);
    }

    @Override
    public void deleteAll() {
        dao.deleteAll();
    }

    @Override
    public void deleteBatch(ArrayList ids) {
        dao.deleteInTx(ids);
    }

    @Override
    public ArrayList<V> loadListSlice(int limit, int offset) {
        return dao.getNextSlice(limit, offset);
    }

    @Override
    public ArrayList loadAll() {
        return dao.getAll();
    }

    @Override
    public V getById(long id) {
        return (V) dao.getById(id);
    }
}
