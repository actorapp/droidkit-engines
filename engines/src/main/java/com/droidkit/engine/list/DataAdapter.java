package com.droidkit.engine.list;

import com.droidkit.engine._internal.sqlite.BinarySerializator;

public interface DataAdapter<V> extends BinarySerializator<V> {

    long getId(V value);

    long getSortKey(V value);

    byte[] serialize(V entity);

    V deserialize(byte[] item);
}
