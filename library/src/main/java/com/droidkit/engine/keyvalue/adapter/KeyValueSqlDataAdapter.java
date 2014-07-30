package com.droidkit.engine.keyvalue.adapter;

import android.database.sqlite.SQLiteDatabase;

import com.droidkit.engine.keyvalue.KeyValueEngineClassConnector;
import com.droidkit.engine.keyvalue.sqlite.KeyValueEngineDao;
import com.droidkit.engine.sqlite.BinarySerializator;

import java.util.ArrayList;

public class KeyValueSqlDataAdapter<V> implements KeyValueEngineDataAdapter<V> {

    private final KeyValueEngineDao<V> dao;

    public KeyValueSqlDataAdapter(SQLiteDatabase database,
                                  String name,
                                  BinarySerializator<V> binarySerializator,
                                  KeyValueEngineClassConnector<V> classConnector) {
        dao = new KeyValueEngineDao<V>(name, database, binarySerializator, classConnector);
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
