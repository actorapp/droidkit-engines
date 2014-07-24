package com.droidkit.engine.list;

public interface ListEngineItemSerializator<V> {

    ListEngineItem serialize(V entity);

    V deserialize(ListEngineItem item);
}
