package com.droidkit.engine.list.sqlite.internal;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.droidkit.engine.list.DataAdapter;
import com.droidkit.engine._internal.sqlite.AbstractDao;

import java.util.ArrayList;


public class ListEngineDao<V> extends AbstractDao<V> {

    private static final String TAG = "ListEngine";

    private static final String TABLENAME_PREFIX = "LIST_ENGINE_ITEM_";
    private static final String COLUMN_LIST_ID = "LIST_ID";
    private static final String COLUMN_ID = "ID";
    private static final String COLUMN_SORT_KEY = "SORT_KEY";
    private static final String COLUMN_BYTES = "BYTES";

    private final long listEngineId;
    private final boolean ascSorting;

    private DataAdapter<V> classConnector;

    public ListEngineDao(String listEngineName,
                         long listEngineId,
                         SQLiteDatabase db,
                         boolean ascSorting,
                         final DataAdapter<V> classConnector) {
        super(TABLENAME_PREFIX + listEngineName,
                db,
                new ListEngineTableStatements(db, TABLENAME_PREFIX + listEngineName),
                classConnector);
        this.listEngineId = listEngineId;
        this.ascSorting = ascSorting;
        this.classConnector = classConnector;
    }

    @Override
    public void createTable() {
        if (!isTableExists()) {
            String constraint = "IF NOT EXISTS ";
            db.execSQL("CREATE TABLE " + constraint + "'" + tableName + "' (" + //
                            "'LIST_ID' INTEGER NOT NULL," + // 0: listId
                            "'ID' INTEGER NOT NULL," + // 1: id
                            "'SORT_KEY' INTEGER NOT NULL," + // 2: sortKey
                            "'BYTES' BLOB NOT NULL," + // 3: bytes
                            "PRIMARY KEY('LIST_ID', 'ID'));"
            );

            db.execSQL("CREATE INDEX " + constraint + "IDX_LIST_ENGINE_ITEM_LIST_ID_SORT_KEY ON '" + tableName + "'" +
                    " (LIST_ID, SORT_KEY);");
            db.execSQL("CREATE INDEX " + constraint + "IDX_LIST_ENGINE_ITEM_ID ON '" + tableName + "'" +
                    " (ID);");
        }
    }

    @Override
    protected void bindValues(SQLiteStatement stmt, V entity) {
        stmt.clearBindings();

        stmt.bindLong(1, listEngineId);

        Long id = classConnector.getId(entity);
        if (id != null) {
            stmt.bindLong(2, id);
        }

        Long sortKey = classConnector.getSortKey(entity);
        if (sortKey != null) {
            stmt.bindLong(3, sortKey);
        }

        byte[] bytes = binarySerializator.serialize(entity);
        if (bytes != null) {
            stmt.bindBlob(4, bytes);
        }
    }

    @Override
    public V readEntity(Cursor cursor) {
        return (V) binarySerializator.deserialize(cursor.isNull(3) ? null : cursor.getBlob(3));
    }

    @Override
    public void deleteByKeyInsideSynchronized(long id, SQLiteStatement stmt) {
        stmt.clearBindings();
        stmt.bindLong(1, listEngineId);
        stmt.bindLong(2, id);
        stmt.execute();
    }

    @Override
    public ArrayList<V> getAll() {
        final String stmt = statements.getAllStatement();
        return loadAllAndCloseCursor(db.rawQuery(stmt,
                new String[]{
                        String.valueOf(listEngineId),
                }
        ));
    }

    @Override
    public V getById(long id) {
        final String stmt = statements.getGetByIdStatement();
        return loadSingleAndCloseCursor(db.rawQuery(stmt,
                new String[]{
                        String.valueOf(listEngineId),
                        String.valueOf(id)
                }
        ));
    }

    public ArrayList<V> getNextSlice(int limit, int offset) {
        final String stmt = ((ListEngineTableStatements) statements).getNextSliceStatement(ascSorting);
        return loadAllAndCloseCursor(db.rawQuery(stmt,
                new String[]{
                        String.valueOf(listEngineId),
                        String.valueOf(limit),
                        String.valueOf(offset)
                }
        ));
    }

}
