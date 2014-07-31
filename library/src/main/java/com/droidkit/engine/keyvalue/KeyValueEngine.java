package com.droidkit.engine.keyvalue;

import android.os.Handler;
import android.os.Looper;
import android.support.v4.util.LruCache;

import com.droidkit.core.Loop;
import com.droidkit.core.Utils;
import com.droidkit.engine.common.ValueCallback;
import com.droidkit.engine.common.ValuesCallback;
import com.droidkit.engine.event.Events;
import com.droidkit.engine.event.NotificationCenter;
import com.droidkit.engine.keyvalue.adapter.KeyValueEngineDataAdapter;
import com.droidkit.engine.sqlite.BinarySerializator;

import java.util.ArrayList;

public class KeyValueEngine<V> {

    private static final String TAG = "KeyValueEngine";

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

    private final KeyValueEngineDataAdapter<V> keyValueEngineDataAdapter;

    private final BinarySerializator<V> binarySerializator;

    private final KeyValueEngineClassConnector<V> keyValueEngineClassConnector;

    public KeyValueEngine(final KeyValueEngineDataAdapter<V> keyValueEngineDataAdapter,
                          final BinarySerializator<V> binarySerializator,
                          final KeyValueEngineClassConnector<V> keyValueEngineClassConnector,
                          final int inMemoryCacheSize) {
        uniqueId = getNextId();
        inMemoryLruCache = new LruCache<Long, V>(inMemoryCacheSize);
        this.loop = LOOPS[uniqueId % LOOPS_COUNT];
        this.keyValueEngineDataAdapter = keyValueEngineDataAdapter;
        this.keyValueEngineClassConnector = keyValueEngineClassConnector;
        this.binarySerializator = binarySerializator;
    }

    public void put(final V value) {
        inMemoryLruCache.put(keyValueEngineClassConnector.getId(value), value);
        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                keyValueEngineDataAdapter.insertOrReplaceSingle(value);
                NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
            }
        });
    }

    public void putSync(final V value) {
        inMemoryLruCache.put(keyValueEngineClassConnector.getId(value), value);
        keyValueEngineDataAdapter.insertOrReplaceSingle(value);
        NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
    }

    public void putAll(final ArrayList<V> values) {
        for(V v : values) {
            inMemoryLruCache.put(keyValueEngineClassConnector.getId(v), v);
        }
        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                keyValueEngineDataAdapter.insertOrReplaceBatch(values);
                NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
            }
        });
    }

    public void putAllSync(final ArrayList<V> values) {
        for(V v : values) {
            inMemoryLruCache.put(keyValueEngineClassConnector.getId(v), v);
        }
        keyValueEngineDataAdapter.insertOrReplaceBatch(values);
        NotificationCenter.getInstance().fireEvent(Events.KEY_VALUE_UPDATE, uniqueId);
    }

    public V getFromMemory(final long id) {
        return inMemoryLruCache.get(id);
    }

    public V getFromDiskSync(final long id) {
        if(Utils.isUIThread()) {
            throw new RuntimeException("getFromDiskSync should be called only from background threads");
        }

        V value = keyValueEngineDataAdapter.getById(id);
        if(value != null) {
            inMemoryLruCache.put(id, value);
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
        return keyValueEngineDataAdapter.loadAll();
    }

    public void clear() {
        inMemoryLruCache.evictAll();
        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                keyValueEngineDataAdapter.deleteAll();
            }
        });
    }

    public int getUniqueId() {
        return uniqueId;
    }
}
