package com.droidkit.engine.keyvalue.sqlite;

import android.database.sqlite.SQLiteDatabase;

import com.droidkit.engine.keyvalue.DataAdapter;
import com.droidkit.engine.keyvalue.StorageAdapter;
import com.droidkit.engine.keyvalue.sqlite.internal.KeyValueEngineDao;

import java.util.ArrayList;

public class SQLiteStorageAdapter<V> implements StorageAdapter<V> {

    private final KeyValueEngineDao<V> dao;

    public SQLiteStorageAdapter(SQLiteDatabase database,
                                String name,
                                DataAdapter<V> adapter) {
        dao = new KeyValueEngineDao<V>(name, database, adapter);
    }

    @Override
    public void insertSingle(V item) {
        dao.insert(item);
    }

    @Override
    public void insertOrReplaceSingle(V item) {
        dao.insertOrReplace(item);
    }

    @Override
    public void deleteSingle(long id) {
        dao.delete(id);
    }

    @Override
    public void insertBatch(ArrayList<V> items) {
        dao.insertInTx(items);
    }

    @Override
    public void insertOrReplaceBatch(ArrayList<V> items) {
        dao.insertOrReplaceInTx(items);
    }

    @Override
    public void deleteBatch(ArrayList<Long> ids) {
        dao.deleteInTx(ids);
    }

    @Override
    public void deleteAll() {
        dao.deleteAll();
    }

    @Override
    public ArrayList<V> loadAll() {
        return dao.getAll();
    }

    @Override
    public V getById(long id) {
        return dao.getById(id);
    }
}
