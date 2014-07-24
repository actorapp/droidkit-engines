package com.droidkit.engine.list.adapter;

import android.database.sqlite.SQLiteDatabase;

import com.droidkit.engine.list.ListEngineItemSerializator;
import com.droidkit.engine.list.sqlite.ListEngineItemDao;

import java.util.ArrayList;

public class FewListsSingleTableDataAdapter<V> implements ListEngineDataAdapter {

    private final String listEngineName;
    private final long listEngineId;
    private final SQLiteDatabase database;
    private final boolean ascSorting;
    private final ListEngineItemDao dao;
    private final ListEngineItemSerializator<V> listEngineItemSerializator;

    public FewListsSingleTableDataAdapter(SQLiteDatabase database, long listEngineId,
                                          String listEngineName, boolean ascSorting,
                                          ListEngineItemSerializator<V> listEngineItemSerializator) {
        this.database = database;
        this.listEngineId = listEngineId;
        this.listEngineName = listEngineName;
        this.ascSorting = ascSorting;
        this.listEngineItemSerializator = listEngineItemSerializator;
        dao = new ListEngineItemDao(listEngineName, listEngineId, database, ascSorting, listEngineItemSerializator);
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
    public V getById(long id) {
        return null;
    }
}
