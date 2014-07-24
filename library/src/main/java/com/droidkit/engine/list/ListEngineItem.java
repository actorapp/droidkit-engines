package com.droidkit.engine.list;

public class ListEngineItem {

    public long id;
    public long sortKey;
    public byte[] data;

    public ListEngineItem(long id, long sortKey, byte[] data) {
        this.id = id;
        this.sortKey = sortKey;
        this.data = data;
    }
}
