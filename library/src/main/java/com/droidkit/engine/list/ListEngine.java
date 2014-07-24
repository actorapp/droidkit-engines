package com.droidkit.engine.list;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.widget.Toast;

import com.droidkit.core.Logger;
import com.droidkit.core.Loop;
import com.droidkit.engine.event.Events;
import com.droidkit.engine.event.NotificationCenter;
import com.droidkit.engine.list.adapter.ListEngineDataAdapter;
import com.droidkit.util.SafeRunnable;
import com.droidkit.util.SortedArrayList;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.ConcurrentHashMap;

public class ListEngine<V> {

    private static final boolean ENABLE_TOAST_LOG = true;

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
    protected final ListEngineDataAdapter listEngineDataAdapter;

    /**
     * Id used in sending of NotificationCenter events
     */
    protected final int uniqueId;

    /**
     * Loop for all in-memory lists operation
     */
    protected final InMemoryListLoop inMemoryListLoop;

    protected final ListEngineItemSerializator<V> listEngineItemSerializator;

    protected final ListEngineClassConnector<V> listEngineClassConnector;

    /**
     * @param comparator             Used for in-memory lists sorting
     * @param dao                    GreenDao DAO
     * @param listEngineQueryBuilder
     */
    public ListEngine(final Context context,
                      final Comparator<V> comparator,
                      final ListEngineDataAdapter listEngineDataAdapter,
                      final ListEngineItemSerializator<V> listEngineItemSerializator,
                      final ListEngineClassConnector<V> listEngineClassConnector) {

        this.uniqueId = getNextId();

        this.loop = LOOPS[uniqueId % LOOPS_COUNT];

        this.inMemorySortedList1 = new SortedArrayList<V>(comparator);
        this.inMemorySortedList2 = new SortedArrayList<V>(comparator);
        this.inMemorySortedList = inMemorySortedList1;

        this.inMemoryMap = new ConcurrentHashMap<Long, V>();
        this.listEngineDataAdapter = listEngineDataAdapter;
        this.listEngineItemSerializator = listEngineItemSerializator;
        this.listEngineClassConnector = listEngineClassConnector;
        this.debugToast = Toast.makeText(context, "", Toast.LENGTH_LONG);

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
    public int getUniqueId() {
        return uniqueId;
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

    protected synchronized void addOrUpdateItem(V value, boolean update, final boolean add) {

        final boolean isAddOnly = !update && add;
        final boolean isUpdateOnly = update && !add;
        final boolean isAddOrUpdate = update && add;

        final long id = listEngineClassConnector.getId(value);

        final V originalValue = inMemoryMap.get(id);

        if ((originalValue != null && isUpdateOnly) || isAddOnly || isAddOrUpdate) {
            modifyInMemoryList(new InMemoryListModification<V>() {
                @Override
                public void modify(SortedArrayList<V> list) {
                    if (originalValue != null && isAddOrUpdate || isUpdateOnly) {
                        list.remove(originalValue);
                    }
                    list.add(originalValue);
                }
            });

            inMemoryMap.put(id, originalValue);

            loop.sendRunnable(new SafeRunnable() {
                @Override
                public void runSafely() {
                    final long dbStart = SystemClock.uptimeMillis();
                    if (isUpdateOnly || isAddOrUpdate) {
                        listEngineDataAdapter.insertOrReplaceSingle(originalValue);
                    } else if (isAddOnly) {
                        listEngineDataAdapter.insertSingle(originalValue);
                    }

//                    showDebugToast("addOrUpdateItem DB: " + (SystemClock.uptimeMillis() - dbStart) + "ms");

                    Logger.d(TAG, "DB addOrUpdateItems in " + (SystemClock.uptimeMillis() - dbStart) + "ms");
                }
            }, 0);
        }
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
            final long id = listEngineClassConnector.getId(val);
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
        });

        loop.sendRunnable(new SafeRunnable() {
            @Override
            public void runSafely() {
                final long dbStart = SystemClock.uptimeMillis();
                if (isUpdateOnly || isAddOrUpdate) {
                    listEngineDataAdapter.insertOrReplaceBatch(values);
                } else if (isAddOnly) {
                    listEngineDataAdapter.insertBatch(values);
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
                    list.remove(val);
                }
            });
        }

        loop.sendRunnable(new SafeRunnable() {
            @Override
            public void runSafely() {
                listEngineDataAdapter.deleteSingle(key);
            }
        }, 0);
    }

    public synchronized int inMemoryListSize() {
        return getActiveList().size();
    }

    public synchronized void loadNextListSlice(final int limit) {
        if (!isDbSliceLoadingInProgress && lastSliceSize > 0) {
            isDbSliceLoadingInProgress = true;

            loop.sendRunnable(new Runnable() {
                @Override
                public void run() {
                    try {
                        Logger.d(TAG, "Loading new slice: offset:" + currentDbOffset + ", limit:" + limit);
                        final long start = SystemClock.uptimeMillis();

                        final ArrayList<V> list = listEngineDataAdapter.loadListSlice(limit, currentDbOffset);

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
                            });

                            for (V val : list) {
                                inMemoryMap.put(listEngineClassConnector.getId(val), val);
                            }
                        }

                    } catch (Exception e) {
                        Logger.e(TAG, "Error loading DB slice", e);
                    }
                    isDbSliceLoadingInProgress = false;
                }
            }, 0);
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
        loop.sendRunnable(new SafeRunnable() {
            @Override
            public void runSafely() {
//                valueCallback.value(listEngineQueryBuilder.createGetByKeyQueryBuilder(dao, key).unique());
            }
        }, 0);
    }

    public synchronized V getValueFromMemoryList(int index) {
        if (index >= 0 && index < getActiveList().size()) {
            return getActiveList().get(index);
        } else {
            return null;
        }
    }

    public void clear() {

        modifyInMemoryList(new InMemoryListModification<V>() {
            @Override
            public void modify(SortedArrayList<V> list) {
                list.clear();
            }
        });

        inMemoryMap.clear();
        loop.sendRunnable(new SafeRunnable() {
            @Override
            public void runSafely() {
                final long dbStart = SystemClock.uptimeMillis();
                listEngineDataAdapter.deleteAll();
                currentDbOffset = 0;
                showDebugToast("DB: " + (SystemClock.uptimeMillis() - dbStart) + "ms");
            }
        }, 0);


    }

    private void showDebugToast(final String text) {
        if (ENABLE_TOAST_LOG) {
            HANDLER.post(new SafeRunnable() {
                @Override
                public void runSafely() {
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

    private synchronized void modifyInMemoryList(InMemoryListModification<V> modification) {
        inMemoryListLoop.sendMessage(Message.obtain(inMemoryListLoop.handler, 0, modification), 0);
    }

    private final Object inMemoryListSync = new Object();

    private class InMemoryListLoop extends Loop {

        public InMemoryListLoop() {
            super("ListEngine-InMemoryList-" + uniqueId);
        }

        @Override
        protected void processMessage(final Message msg) {
            synchronized (inMemoryListSync) {

                final InMemoryListModification<V> modification = (InMemoryListModification<V>) msg.obj;

                final SortedArrayList<V> beginInactiveList = getInactiveList();

                modification.modify(beginInactiveList);

                HANDLER.postDelayed(new SafeRunnable() {
                    @Override
                    public void runSafely() {
                        synchronized (inMemoryListSync) {
                            switchActiveList();
                            Logger.d(TAG, "inMemorySortedList1.size():" + inMemorySortedList1.size() + ", inMemorySortedList2.size():" + inMemorySortedList2.size());
                            NotificationCenter.getInstance().fireEvent(Events.LIST_ENGINE_UI_LIST_UPDATE, uniqueId);
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
                    HANDLER.postDelayed(new SafeRunnable() {
                        @Override
                        public void runSafely() {
                            synchronized (inMemoryListSync) {
                                modification.modify(getActiveList());
                                NotificationCenter.getInstance().fireEvent(Events.LIST_ENGINE_UI_LIST_UPDATE, uniqueId);
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
