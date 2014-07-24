package com.droidkit.engine.list.sqlite;

import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.droidkit.core.Logger;
import com.droidkit.engine.list.ListEngineItem;
import com.droidkit.engine.list.ListEngineItemSerializator;

import java.util.ArrayList;


public class ListEngineItemDao<V> {

    private static final String TAG = "ListEngineItemDao";

    private static final String TABLENAME_PREFIX = "LIST_ENGINE_ITEM_";
    private static final String COLUMN_LIST_ID = "LIST_ID";
    private static final String COLUMN_ID = "ID";
    private static final String COLUMN_SORT_KEY = "SORT_KEY";
    private static final String COLUMN_BYTES = "BYTES";
    private static final String[] ALL_COLUMNS = new String[]{
            COLUMN_LIST_ID,
            COLUMN_ID,
            COLUMN_SORT_KEY,
            COLUMN_BYTES
    };

    private final SQLiteDatabase db;
    private final long listEngineId;
    private final String tableName;
    private final boolean ascSorting;
    private final ListEngineItemSerializator<V> listEngineItemSerializator;

    private TableStatements statements;

    public ListEngineItemDao(String listEngineName,
                             long listEngineId, 
                             SQLiteDatabase db,
                             boolean ascSorting,
                             ListEngineItemSerializator<V> listEngineItemSerializator) {
        this.listEngineId = listEngineId;
        this.db = db;
        this.tableName = TABLENAME_PREFIX + listEngineName;
        this.ascSorting = ascSorting;
        this.listEngineItemSerializator = listEngineItemSerializator;
        statements = new TableStatements(db, tableName,
                ALL_COLUMNS,
                new String[]{
                        COLUMN_LIST_ID,
                        COLUMN_ID
                },
                COLUMN_LIST_ID,
                COLUMN_SORT_KEY);
        createTable();
    }

    private boolean isTableExists() {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        if(cursor!=null) {
            if(cursor.getCount()>0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    /** Creates the underlying database table. */
    public void createTable() {
        if(!isTableExists()) {
            String constraint = "IF NOT EXISTS ";
            db.execSQL("CREATE TABLE " + constraint + "'" + tableName + "' (" + //
                    "'LIST_ID' INTEGER," + // 0: listId
                    "'ID' INTEGER," + // 1: id
                    "'SORT_KEY' INTEGER," + // 2: sortKey
                    "'BYTES' BLOB);"); // 3: bytes
            // Add Indexes
            db.execSQL("CREATE INDEX " + constraint + "IDX_LIST_ENGINE_ITEM_LIST_ID_SORT_KEY ON " + tableName +
                    " (LIST_ID, SORT_KEY);");
            db.execSQL("CREATE INDEX " + constraint + "IDX_LIST_ENGINE_ITEM_ID ON " + tableName +
                    " (ID);");
        }
    }

    /** Drops the underlying database table. */
    public void dropTable() {
        String sql = "DROP TABLE " + "IF EXISTS " + "'" + tableName + "'";
        db.execSQL(sql);
    }


    /**
     * @return row ID of newly inserted entity
     */
    public long insert(V entity) {
        return executeInsert(entity, statements.getInsertStatement());
    }

    /**
     * @return row ID of newly inserted entity
     */
    public long insertOrReplace(V entity) {
        return executeInsert(entity, statements.getInsertOrReplaceStatement());
    }

    private long executeInsert(V entity, SQLiteStatement stmt) {
        long rowId;
        if (db.isDbLockedByCurrentThread()) {
            synchronized (stmt) {
                bindValues(stmt, listEngineItemSerializator.serialize(entity));
                rowId = stmt.executeInsert();
            }
        } else {
            // Do TX to acquire a connection before locking the stmt to avoid deadlocks
            db.beginTransaction();
            try {
                synchronized (stmt) {
                    bindValues(stmt, listEngineItemSerializator.serialize(entity));
                    rowId = stmt.executeInsert();
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return rowId;
    }

    /** Deletes the given entity from the database. */
    public void delete(long id) {
        SQLiteStatement stmt = statements.getDeleteStatement();
        if (db.isDbLockedByCurrentThread()) {
            synchronized (stmt) {
                deleteByKeyInsideSynchronized(id, stmt);
            }
        } else {
            // Do TX to acquire a connection before locking the stmt to avoid deadlocks
            db.beginTransaction();
            try {
                synchronized (stmt) {
                    deleteByKeyInsideSynchronized(id, stmt);
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
    }

    public void deleteAll() {
        db.execSQL("DELETE FROM '" + tableName + "'");
    }

    private void deleteByKeyInsideSynchronized(long id, SQLiteStatement stmt) {
        stmt.bindLong(1, listEngineId);
        stmt.bindLong(2, id);
        stmt.execute();
    }

    /**
     * Inserts the given entities in the database using a transaction.
     */
    public void insertInTx(ArrayList<V> entities) {
        SQLiteStatement stmt = statements.getInsertStatement();
        executeInsertInTx(stmt, entities);
    }

    /**
     * Inserts or replaces the given entities in the database using a transaction. The given entities will become
     * tracked if the PK is set.
     *
     * @param entities
     *            The entities to insert.
     */
    public void insertOrReplaceInTx(ArrayList<V> entities) {
        SQLiteStatement stmt = statements.getInsertOrReplaceStatement();
        executeInsertInTx(stmt, entities);
    }


    private void executeInsertInTx(SQLiteStatement stmt, ArrayList<V> entities) {
        db.beginTransaction();
        try {
            synchronized (stmt) {
                for (V entity : entities) {
                    bindValues(stmt, listEngineItemSerializator.serialize(entity));
                    stmt.execute();
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public void deleteInTx(ArrayList<Long> entities) {
        SQLiteStatement stmt = statements.getDeleteStatement();
        db.beginTransaction();
        try {
            synchronized (stmt) {
                if (entities != null) {
                    for (long id : entities) {
                        deleteByKeyInsideSynchronized(id, stmt);
                    }
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    public ArrayList<V> getNextSlice(int limit, int offset) {
        final String stmt = statements.getNextSliceStatement(ascSorting);
        return loadAllAndCloseCursor(db.rawQuery(stmt,
                new String[]{
                String.valueOf(listEngineId),
                String.valueOf(limit),
                String.valueOf(offset)
        }));
    }

    private ArrayList<V> loadAllAndCloseCursor(Cursor cursor) {
        try {
            return loadAllFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    private ArrayList<V> loadAllFromCursor(Cursor cursor) {
        int count = cursor.getCount();
        ArrayList<V> list = new ArrayList<V>(count);
        if (cursor instanceof CrossProcessCursor) {
            CursorWindow window = ((CrossProcessCursor) cursor).getWindow();
            if (window != null) { // E.g. Roboelectric has no Window at this point
                if (window.getNumRows() == count) {
                    cursor = new FastCursor(window);
                } else {
                    Logger.d("Window vs. result size: " + window.getNumRows() + "/" + count);
                }
            }
        }

        final ArrayList<ListEngineItem> rawList = new ArrayList<ListEngineItem>(count);


        if (cursor.moveToFirst()) {
            do {
                rawList.add(new ListEngineItem( //
                        cursor.isNull(1) ? null : cursor.getLong(1), // id
                        cursor.isNull(2) ? null : cursor.getLong(2), // sortKey
                        cursor.isNull(3) ? null : cursor.getBlob(3))); // bytes);
//                list.add(loadCurrent(cursor, 0, false));
            } while (cursor.moveToNext());
        }
        final long start = System.currentTimeMillis();
        for(ListEngineItem i : rawList) {
            list.add((V) listEngineItemSerializator.deserialize(i));
        }
        Logger.d("tmp", "Deserealization time " + (System.currentTimeMillis() - start) + "ms");
        return list;
    }

    private  V loadCurrent(Cursor cursor) {
        V entity = readEntity(cursor);
        return entity;
    }

    private V readEntity(Cursor cursor) {
        ListEngineItem entity = new ListEngineItem( //
                cursor.isNull(1) ? null : cursor.getLong(1), // id
                cursor.isNull(2) ? null : cursor.getLong(2), // sortKey
                cursor.isNull(3) ? null : cursor.getBlob(3) // bytes
        );
        return (V) listEngineItemSerializator.deserialize(entity);
    }

    private void bindValues(SQLiteStatement stmt, ListEngineItem entity) {
        stmt.clearBindings();

        stmt.bindLong(1, listEngineId);

        Long id = entity.id;
        if (id != null) {
            stmt.bindLong(2, id);
        }
 
        Long sortKey = entity.sortKey;
        if (sortKey != null) {
            stmt.bindLong(3, sortKey);
        }
 
        byte[] bytes = entity.data;
        if (bytes != null) {
            stmt.bindBlob(4, bytes);
        }
    }

}
