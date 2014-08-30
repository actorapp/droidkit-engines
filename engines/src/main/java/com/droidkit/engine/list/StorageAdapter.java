package com.droidkit.engine.list;

import java.util.ArrayList;

public interface StorageAdapter<V>  {

    void insertSingle(V item);

    void insertOrReplaceSingle(V item);

    void deleteSingle(long id);

    void insertBatch(ArrayList<V> items);

    void insertOrReplaceBatch(ArrayList<V> items);

    void deleteBatch(ArrayList<Long> ids);

    void deleteAll();

    ArrayList<V> loadListSlice(int limit, int offset);

    ArrayList<V> loadAll();

    V getById(long id);
}
