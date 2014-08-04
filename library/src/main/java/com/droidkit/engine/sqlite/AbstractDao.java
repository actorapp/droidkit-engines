package com.droidkit.engine.sqlite;

import android.database.CrossProcessCursor;
import android.database.Cursor;
import android.database.CursorWindow;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.droidkit.core.Logger;

import java.util.ArrayList;

public abstract class AbstractDao<V> {

    protected static final String TAG = "AbstractDao";

    public final SQLiteDatabase db;
    public final String tableName;
    public final SqlStatements statements;
    public final BinarySerializator<V> binarySerializator;

    protected AbstractDao(String tableName, SQLiteDatabase db, SqlStatements statements, BinarySerializator<V> serializator) {
        this.tableName = tableName;
        this.db = db;
        this.statements = statements;
        this.binarySerializator = serializator;
        createTable();
    }

    /**
     * Creates the underlying database table.
     */
    protected abstract void createTable();

    protected abstract void bindValues(SQLiteStatement stmt, V entity);

    protected abstract V readEntity(Cursor cursor);

    protected abstract void deleteByKeyInsideSynchronized(long id, SQLiteStatement stmt);

    public abstract ArrayList<V> getAll();

    public abstract V getById(long id);

    protected boolean isTableExists() {
        Cursor cursor = db.rawQuery("select DISTINCT tbl_name from sqlite_master where tbl_name = '" + tableName + "'", null);
        if (cursor != null) {
            if (cursor.getCount() > 0) {
                cursor.close();
                return true;
            }
            cursor.close();
        }
        return false;
    }

    /**
     * Drops the underlying database table.
     */
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
                bindValues(stmt, entity);
                rowId = stmt.executeInsert();
            }
        } else {
            // Do TX to acquire a connection before locking the stmt to avoid deadlocks
            db.beginTransaction();
            try {
                synchronized (stmt) {
                    bindValues(stmt, entity);
                    rowId = stmt.executeInsert();
                }
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }
        return rowId;
    }

    /**
     * Deletes the given entity from the database.
     */
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
     * @param entities The entities to insert.
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
                    bindValues(stmt, entity);
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

    protected V loadSingleAndCloseCursor(Cursor cursor) {
        try {
            V item = null;
            if (cursor.moveToFirst()) {
                item = loadCurrent(cursor);
            }
            return item;
        } finally {
            cursor.close();
        }
    }

    protected ArrayList<V> loadAllAndCloseCursor(Cursor cursor) {
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
            if (window != null) {
                if (window.getNumRows() == count) {
                    cursor = new FastCursor(window);
                } else {
                    Logger.d("Window vs. result size: " + window.getNumRows() + "/" + count);
                }
            }
        }

        final long start = System.currentTimeMillis();
        if (cursor.moveToFirst()) {
            do {
                list.add(loadCurrent(cursor));
            } while (cursor.moveToNext());
        }
        Logger.d(TAG, "Deserealization time " + (System.currentTimeMillis() - start) + "ms");
        return list;
    }

    private V loadCurrent(Cursor cursor) {
        V entity = readEntity(cursor);
        return entity;
    }

}
