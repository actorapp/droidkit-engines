package com.droidkit.engine.keyvalue;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;

import com.droidkit.engine._internal.core.Loop;
import com.droidkit.engine._internal.core.Utils;
import com.droidkit.engine.common.ValueCallback;
import com.droidkit.engine.common.ValuesCallback;
import com.droidkit.engine.event.Events;
import com.droidkit.engine.event.NotificationCenter;

import java.util.ArrayList;

public class KeyValueEngine<V> {

    private static final String TAG = "KeyValueEngine";

    private static final int DEFAULT_MEMORY_CACHE = 128;

    private static final int MAX_LRU_CACHE_SIZE = 100;
    private static final int LOOPS_COUNT = 2;
    private static final Loop[] LOOPS = new Loop[LOOPS_COUNT];
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    static {
        for (int i = 0; i < LOOPS_COUNT; ++i) {
            LOOPS[i] = new Loop("KeyValueEngine-Loop-" + i);
            LOOPS[i].setPriority(Thread.MIN_PRIORITY);
            LOOPS[i].start();
        }
    }

    private static volatile int lastId = 0;

    private static synchronized int getNextId() {
        lastId++;
        return lastId;
    }


    /**
     * Loop for all database operations
     */
    protected final Loop loop;

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
        this.uniqueId = getNextId();
        this.inMemoryLruCache = new LruCache<Long, V>(inMemoryCacheSize);
        this.loop = LOOPS[uniqueId % LOOPS_COUNT];
        this.storageAdapter = storageAdapter;
        this.dataAdapter = dataAdapter;
    }

    public void put(final V value) {
        inMemoryLruCache.put(dataAdapter.getId(value), value);
        loop.postRunnable(new Runnable() {
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
        loop.postRunnable(new Runnable() {
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
        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                callback.value(getFromDiskSync(id));
            }
        });
    }

    public void getAllFromDisk(final ValuesCallback<V> callback) {
        loop.postRunnable(new Runnable() {
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
        loop.postRunnable(new Runnable() {
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
        loop.postRunnable(new Runnable() {
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
