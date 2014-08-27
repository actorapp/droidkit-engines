package com.droidkit.engine.list;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Toast;

import com.droidkit.engine._internal.core.Logger;
import com.droidkit.engine._internal.core.Loop;
import com.droidkit.engine._internal.core.Utils;
import com.droidkit.engine.common.ValueCallback;
import com.droidkit.engine.event.Events;
import com.droidkit.engine.event.NotificationCenter;
import com.droidkit.engine._internal.util.SortedArrayList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

public class ListEngine<V> {

    private static final boolean ENABLE_TOAST_LOG = false;

    private static final String TAG = "ListEngine";

    private static final int LOOPS_COUNT = 2;
    private static final Loop[] LOOPS = new Loop[LOOPS_COUNT];
    private static final Handler HANDLER = new Handler(Looper.getMainLooper());

    static {
        for (int i = 0; i < LOOPS_COUNT; ++i) {
            LOOPS[i] = new Loop("ListEngine-Loop-" + i);
            LOOPS[i].setPriority(Thread.MIN_PRIORITY);
            LOOPS[i].start();
        }
    }

    private static volatile int lastId = 0;

    private static synchronized int getNextId() {
        lastId++;
        return lastId;
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Toast for screen debug output
     */
    protected Toast debugToast;


    /**
     * Loop for all database operations
     */
    protected final Loop loop;


    /**
     * Map for storing values
     */
    protected final ConcurrentHashMap<Long, V> inMemoryMap;


    /**
     * List for storing values, always synchronized with inMemorySortedList2
     */
    protected final SortedArrayList<V> inMemorySortedList1;


    /**
     * List for storing values, always synchronized with inMemorySortedList1
     */
    protected final SortedArrayList<V> inMemorySortedList2;


    /**
     * Database interface
     */
    protected final StorageAdapter storageAdapter;

    /**
     * Id used in sending of NotificationCenter events
     */
    protected final int listEngineId;

    /**
     * Loop for all in-memory lists operation
     */
    protected final InMemoryListLoop inMemoryListLoop;

    /**
     * Adapter of data for Engine
     */
    protected final DataAdapter<V> dataAdapter;

    /**
     * Creating ListEngine instance
     *
     * @param context        android context
     * @param storageAdapter storage adapter
     * @param dataAdapter    data adapter
     */
    public ListEngine(final StorageAdapter storageAdapter,
                      final DataAdapter<V> dataAdapter) {

        this.listEngineId = getNextId();

        this.loop = LOOPS[Math.abs(this.listEngineId) % LOOPS_COUNT];

        Comparator<V> comparator = new Comparator<V>() {
            @Override
            public int compare(V lhs, V rhs) {
                long lKey = dataAdapter.getSortKey(lhs);
                long rKey = dataAdapter.getSortKey(rhs);

                if (lKey > rKey) {
                    return 1;
                } else if (lKey < rKey) {
                    return -1;
                } else {
                    return 0;
                }
            }
        };

        this.inMemorySortedList1 = new SortedArrayList<V>(comparator);
        this.inMemorySortedList2 = new SortedArrayList<V>(comparator);
        this.inMemorySortedList = inMemorySortedList1;

        this.inMemoryMap = new ConcurrentHashMap<Long, V>();
        this.storageAdapter = storageAdapter;
        this.dataAdapter = dataAdapter;

        this.inMemoryListLoop = new InMemoryListLoop();
        this.inMemoryListLoop.setPriority(Thread.MIN_PRIORITY);
        this.inMemoryListLoop.start();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////

    protected volatile int lastSliceSize = 1;

    protected volatile int currentDbOffset = 0;

    protected volatile boolean isDbSliceLoadingInProgress = false;

    protected volatile SortedArrayList<V> inMemorySortedList;

    ////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * @return Id unique for current ListEngine instance, must be used in NotificationListeners
     */
    public int getListEngineId() {
        return listEngineId;
    }

    public synchronized void addItem(final V value) {
        addOrUpdateItem(value, false, true);
    }

    public synchronized void updateItem(final V value) {
        addOrUpdateItem(value, true, false);
    }

    public synchronized void addOrUpdateItem(final V value) {
        addOrUpdateItem(value, true, true);
    }

    protected synchronized void addOrUpdateItem(final V value, boolean update, final boolean add) {

        final boolean isAddOnly = !update && add;
        final boolean isUpdateOnly = update && !add;
        final boolean isAddOrUpdate = update && add;

        final long id = dataAdapter.getId(value);

        modifyInMemoryList(new InMemoryListModification<V>() {
            @Override
            public void modify(SortedArrayList<V> list) {
                if (isAddOrUpdate || isUpdateOnly) {
                    Iterator<V> it = list.iterator();
                    while (it.hasNext()) {
                        V value = it.next();
                        if (dataAdapter.getId(value) == id) {
                            it.remove();
                            break;
                        }
                    }
                }
                list.add(value);
            }
        }, 1);

        inMemoryMap.put(id, value);

        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                final long dbStart = SystemClock.uptimeMillis();
                if (isUpdateOnly || isAddOrUpdate) {
                    storageAdapter.insertOrReplaceSingle(value);
                } else if (isAddOnly) {
                    storageAdapter.insertSingle(value);
                }

                Logger.d(TAG, "DB addOrUpdateItems in " + (SystemClock.uptimeMillis() - dbStart) + "ms");
            }
        }, 0);
    }

    public synchronized void addItems(final ArrayList<V> values) {
        addOrUpdateItems(values, false, true);
    }

    public synchronized void updateItems(final ArrayList<V> values) {
        addOrUpdateItems(values, true, false);
    }

    public synchronized void addOrUpdateItems(final ArrayList<V> values) {
        addOrUpdateItems(values, true, true);
    }

    protected synchronized void addOrUpdateItems(final ArrayList<V> values, boolean update, final boolean add) {

        final boolean isAddOnly = !update && add;
        final boolean isUpdateOnly = update && !add;
        final boolean isAddOrUpdate = update && add;

        final ArrayList<V> toRemove = new ArrayList<V>();

        for (V val : values) {
            final long id = dataAdapter.getId(val);
            final V originalValue = inMemoryMap.get(id);

            if ((originalValue != null && isUpdateOnly) || isAddOnly || isAddOrUpdate) {
                if (originalValue != null && isAddOrUpdate || isUpdateOnly) {
                    toRemove.add(originalValue);
                }
                inMemoryMap.put(id, val);
            }
        }

        modifyInMemoryList(new InMemoryListModification<V>() {
            @Override
            public void modify(SortedArrayList<V> list) {
                list.removeAll(toRemove);
                list.addAll(values);
            }
        }, values.size());

        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                final long dbStart = SystemClock.uptimeMillis();
                if (isUpdateOnly || isAddOrUpdate) {
                    storageAdapter.insertOrReplaceBatch(values);
                } else if (isAddOnly) {
                    storageAdapter.insertBatch(values);
                }
                showDebugToast("addOrUpdateItems DB: " + (SystemClock.uptimeMillis() - dbStart) + "ms");
                Logger.d(TAG, "DB addOrUpdateItems in " + (SystemClock.uptimeMillis() - dbStart) + "ms");

            }
        }, 0);
    }

    public synchronized void removeItem(final long key) {
        final V val = inMemoryMap.remove(key);
        if (val != null) {
            modifyInMemoryList(new InMemoryListModification<V>() {
                @Override
                public void modify(SortedArrayList<V> list) {
                    Iterator<V> it = list.iterator();
                    while (it.hasNext()) {
                        V value = it.next();
                        if (dataAdapter.getId(value) == key) {
                            it.remove();
                            break;
                        }
                    }
                }
            }, -1);
        }

        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                storageAdapter.deleteSingle(key);
            }
        }, 0);
    }

    public synchronized int getCountInMemoryList() {
        return getActiveList().size();
    }

    public synchronized void loadNextListSlice(final int limit) {
        if (!isDbSliceLoadingInProgress && lastSliceSize > 0) {
            isDbSliceLoadingInProgress = true;

            loop.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        Logger.d(TAG, "Loading new slice: offset:" + currentDbOffset + ", limit:" + limit);
                        final long start = SystemClock.uptimeMillis();

                        final ArrayList<V> list = storageAdapter.loadListSlice(limit, currentDbOffset);

                        if (list != null) {
                            lastSliceSize = list.size();
                            currentDbOffset += lastSliceSize;

                            Logger.d(TAG, "Loaded " + lastSliceSize + " items in " + (SystemClock.uptimeMillis() - start) + "ms");
                            showDebugToast("loadNextListSlice(" + limit + ") in " + (SystemClock.uptimeMillis() - start) + "ms");

                            modifyInMemoryList(new InMemoryListModification<V>() {
                                @Override
                                public void modify(SortedArrayList<V> targetList) {
                                    targetList.addAll(list);
                                }
                            }, list.size());

                            for (V val : list) {
                                inMemoryMap.put(dataAdapter.getId(val), val);
                            }
                        }

                    } catch (Exception e) {
                        Logger.e(TAG, "Error loading DB slice", e);
                    }
                    isDbSliceLoadingInProgress = false;
                }
            });
        }
    }

    public synchronized void loadAll() {
        if (!isDbSliceLoadingInProgress && lastSliceSize > 0) {
            isDbSliceLoadingInProgress = true;

            loop.postRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        Logger.d(TAG, "Loading whole list");
                        final long start = SystemClock.uptimeMillis();

                        final ArrayList<V> list = storageAdapter.loadAll();

                        if (list != null) {
                            lastSliceSize = list.size();
                            currentDbOffset += lastSliceSize;

                            Logger.d(TAG, "Loaded " + lastSliceSize + " items in " + (SystemClock.uptimeMillis() - start) + "ms");
                            showDebugToast("loadAll() in " + (SystemClock.uptimeMillis() - start) + "ms");

                            modifyInMemoryList(new InMemoryListModification<V>() {
                                @Override
                                public void modify(SortedArrayList<V> targetList) {
                                    targetList.clear();
                                    targetList.addAll(list);
                                }
                            }, list.size());

                            for (V val : list) {
                                inMemoryMap.put(dataAdapter.getId(val), val);
                            }
                        }

                    } catch (Exception e) {
                        Logger.e(TAG, "Error loading DB slice", e);
                    }
                    isDbSliceLoadingInProgress = false;
                }
            });
        }
    }

    /**
     * Try to find value in memory by key
     *
     * @param key
     * @return Value
     */
    public synchronized V getValueFromMemory(long key) {
        V value = inMemoryMap.get(key);
        return value;
    }

    public synchronized void getValueFromDb(final long key, final ValueCallback<V> valueCallback) {
        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                V v = (V) storageAdapter.getById(key);
                inMemoryMap.put(key, v);
                valueCallback.value(v);
            }
        });
    }

    public synchronized V getValueFromMemoryList(int index) {
        if (index >= 0 && index < getActiveList().size()) {
            return getActiveList().get(index);
        } else {
            return null;
        }
    }

    public synchronized void clear() {

        clearInMemory();

        loop.postRunnable(new Runnable() {
            @Override
            public void run() {
                final long dbStart = SystemClock.uptimeMillis();
                storageAdapter.deleteAll();
                currentDbOffset = 0;
                showDebugToast("DB: " + (SystemClock.uptimeMillis() - dbStart) + "ms");
            }
        }, 0);
    }

    public synchronized void clearMemoryInternal() {
        final int changeSize = inMemorySortedList1.size();
        inMemorySortedList1.clear();
        inMemorySortedList2.clear();
        inMemoryMap.clear();
        lastSliceSize = 1;
        currentDbOffset = 0;
        NotificationCenter.getInstance().fireEvent(Events.LIST_ENGINE_UI_LIST_UPDATE, listEngineId, new Integer[]{changeSize});
    }

    private synchronized void clearInMemory() {
        if (Utils.isUIThread()) {
            clearMemoryInternal();
        } else {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    clearMemoryInternal();
                }
            });
        }
    }

    private void showDebugToast(final String text) {
        if (ENABLE_TOAST_LOG) {
            HANDLER.post(new Runnable() {
                @Override
                public void run() {
                    debugToast.setText(text);
                    debugToast.show();
                }
            });
        }
    }

    private void switchActiveList() {
        synchronized (inMemoryListSync) {
            if (inMemorySortedList == inMemorySortedList1) {
                inMemorySortedList = inMemorySortedList2;
            } else {
                inMemorySortedList = inMemorySortedList1;
            }
        }
    }

    private SortedArrayList<V> getInactiveList() {
        synchronized (inMemoryListSync) {
            if (inMemorySortedList == inMemorySortedList1) {
                return inMemorySortedList2;
            } else {
                return inMemorySortedList1;
            }
        }
    }

    private SortedArrayList<V> getActiveList() {
        synchronized (inMemoryListSync) {
            return inMemorySortedList;
        }
    }

    private synchronized void modifyInMemoryList(InMemoryListModification<V> modification, int changeSize) {
        inMemoryListLoop.postMessage(Message.obtain(inMemoryListLoop.handler, changeSize, modification), 0);
    }

    private final Object inMemoryListSync = new Object();

    private class InMemoryListLoop extends Loop {

        public InMemoryListLoop() {
            super("ListEngine-InMemoryList-" + listEngineId);
        }

        @Override
        protected void processMessage(final Message msg) {
            synchronized (inMemoryListSync) {

                final InMemoryListModification<V> modification = (InMemoryListModification<V>) msg.obj;

                final SortedArrayList<V> beginInactiveList = getInactiveList();

                modification.modify(beginInactiveList);

                HANDLER.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        synchronized (inMemoryListSync) {
                            switchActiveList();
                            Logger.d(TAG, "inMemorySortedList1.size():" + inMemorySortedList1.size() + ", inMemorySortedList2.size():" + inMemorySortedList2.size());
                            NotificationCenter.getInstance().fireEvent(Events.LIST_ENGINE_UI_LIST_UPDATE, listEngineId, new Integer[]{msg.what});
                            inMemoryListSync.notifyAll();
                        }
                    }
                }, 5);

                try {
                    inMemoryListSync.wait(5000);
                } catch (InterruptedException e) {
                    Logger.d(e);
                }

                final SortedArrayList<V> endInactiveList = getInactiveList();
                if (beginInactiveList != endInactiveList) {
                    modification.modify(endInactiveList);
                } else {
                    //SOMETHING WENT WRONG!
                    Logger.e(TAG, "Error in memories list sync");
                    Logger.e(TAG, "inMemorySortedList1.size():" + inMemorySortedList1.size() + ", inMemorySortedList2.size():" + inMemorySortedList2.size());
                    HANDLER.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            synchronized (inMemoryListSync) {
                                modification.modify(getActiveList());
                                NotificationCenter.getInstance().fireEvent(Events.LIST_ENGINE_UI_LIST_UPDATE, listEngineId, new Integer[]{msg.what});
                                inMemoryListSync.notifyAll();
                            }
                        }
                    }, 5);
                    try {
                        inMemoryListSync.wait(5000);
                    } catch (InterruptedException e) {
                        Logger.d(e);
                    }
                }
            }
        }
    }

    private interface InMemoryListModification<V> {
        void modify(final SortedArrayList<V> list);
    }
}
