package com.droidkit.engine.sqlite;

public interface BinarySerializator<V> {

    byte[] serialize(V entity);

    V deserialize(byte[] item);
}
