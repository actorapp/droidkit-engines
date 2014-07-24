package com.droidkit.engine.list;

public interface ListEngineClassConnector<V> {

    long getId(V value);

    long getSortKey(V value);

}
