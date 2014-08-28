package com.droidkit.engine.keyvalue;

import android.support.v4.util.LruCache;

import com.droidkit.actors.ActorRef;
import com.droidkit.actors.ActorSystem;
import com.droidkit.engine._internal.RunnableActor;
import com.droidkit.engine._internal.util.Utils;
import com.droidkit.engine.common.ValueCallback;
import com.droidkit.engine.common.ValuesCallback;
import com.droidkit.engine.event.Events;
import com.droidkit.engine.event.NotificationCenter;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class KeyValueEngine<V> {

    private static final String TAG = "KeyValueEngine";

    private static final int DEFAULT_MEMORY_CACHE = 128;

    private static final AtomicInteger NEXT_ID = new AtomicInteger(1);

    /**
     * Loop for all database operations
     */
    protected final ActorRef dbActor;

    /**
     * Id used in sending of NotificationCenter events
     */
    protected final int uniqueId;

    private LruCache<Long, V> inMemoryLruCache;

    private final StorageAdapter<V> storageAdapter;

    private final DataAdapter<V> dataAdapter;

    public KeyValueEngine(StorageAdapter<V> storageAdapter,
                          DataAdapter<V> dataAdapter) {
        this(storageAdapter, dataAdapter, DEFAULT_MEMORY_CACHE);

    }

    public KeyValueEngine(StorageAdapter<V> storageAdapter,
                          DataAdapter<V> dataAdapter,
                          int inMemoryCacheSize) {
        this.uniqueId = NEXT_ID.getAndIncrement();
        this.inMemoryLruCache = new LruCache<Long, V>(inMemoryCacheSize);
        this.storageAdapter = storageAdapter;
        this.dataAdapter = dataAdapter;
        this.dbActor = ActorSystem.system().actorOf(RunnableActor.class, "key_value_db_" + uniqueId);
    }

    public void put(final V value) {
        inMemoryLruCache.put(dataAdapter.getId(value), value);
        dbActor.send(new Runnable() {
            @Override
            public void run() {
                storageAdapter.insertOrReplaceSingle(value);
                NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
            }
        });
    }

    public void putSync(final V value) {
        inMemoryLruCache.put(dataAdapter.getId(value), value);
        storageAdapter.insertOrReplaceSingle(value);
        NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
    }

    public void putAll(final ArrayList<V> values) {
        for (V v : values) {
            inMemoryLruCache.put(dataAdapter.getId(v), v);
        }
        dbActor.send(new Runnable() {
            @Override
            public void run() {
                storageAdapter.insertOrReplaceBatch(values);
                NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
            }
        });
    }

    public void putAllSync(final ArrayList<V> values) {
        for (V v : values) {
            inMemoryLruCache.put(dataAdapter.getId(v), v);
        }
        storageAdapter.insertOrReplaceBatch(values);
        NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
    }

    public V getFromMemory(final long id) {
        return inMemoryLruCache.get(id);
    }

    public V getFromDiskSync(final long id) {
        if (Utils.isUIThread()) {
            throw new RuntimeException("getFromDiskSync should be called only from background threads");
        }

        V value = storageAdapter.getById(id);
        if (value != null) {
            inMemoryLruCache.put(id, value);
        }
        return value;
    }

    public V getSync(final long id) {
        V value = getFromMemory(id);
        if (value == null) {
            value = getFromDiskSync(id);
        }
        return value;
    }

    public void getFromDisk(final long id, final ValueCallback<V> callback) {
        dbActor.send(new Runnable() {
            @Override
            public void run() {
                callback.value(getFromDiskSync(id));
            }
        });
    }

    public void getAllFromDisk(final ValuesCallback<V> callback) {
        dbActor.send(new Runnable() {
            @Override
            public void run() {
                callback.values(getAllFromDiskSync());
            }
        });
    }

    public ArrayList<V> getAllFromDiskSync() {
        return storageAdapter.loadAll();
    }

    public void clear() {
        inMemoryLruCache.evictAll();
        dbActor.send(new Runnable() {
            @Override
            public void run() {
                storageAdapter.deleteAll();
            }
        });
    }

    public void clearSync() {
        inMemoryLruCache.evictAll();
        storageAdapter.deleteAll();
    }

    public void remove(final long id) {
        inMemoryLruCache.remove(id);
        dbActor.send(new Runnable() {
            @Override
            public void run() {
                storageAdapter.deleteSingle(id);
            }
        });
    }

    public void removeSync(final long id) {
        inMemoryLruCache.remove(id);
        storageAdapter.deleteSingle(id);
    }

    public int getUniqueId() {
        return uniqueId;
    }
}
