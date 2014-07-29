package com.droidkit.engine.list.sqlite;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

/** Helper class to create SQL statements for specific tables (used by greenDAO internally). */
public class TableStatements {
    private final SQLiteDatabase db;
    private final String tablename;
    private final String[] allColumns;
    private final String[] idColumns;
    private final String id;
    private final String listEngineId;
    private final String sortKey;

    private SQLiteStatement insertStatement;
    private SQLiteStatement insertOrReplaceStatement;
    private SQLiteStatement updateStatement;
    private SQLiteStatement deleteStatement;

    private String nextSliceStatementAsc;
    private String nextSliceStatementDesc;
    private String getByIdStatement;
    private String allStatement;

    public TableStatements(SQLiteDatabase db, String tablename, String[] allColumns,
                           String[] idColumns, String id, String listEngineId, String sortKey) {
        this.db = db;
        this.tablename = tablename;
        this.allColumns = allColumns;
        this.idColumns = idColumns;
        this.id = id;
        this.listEngineId = listEngineId;
        this.sortKey = sortKey;
    }

    public SQLiteStatement getInsertStatement() {
        if (insertStatement == null) {
            String sql = SqlUtils.createSqlInsert("INSERT INTO ", tablename, allColumns);
            insertStatement = db.compileStatement(sql);
        }
        return insertStatement;
    }

    public SQLiteStatement getInsertOrReplaceStatement() {
        if (insertOrReplaceStatement == null) {
            String sql = SqlUtils.createSqlInsert("INSERT OR REPLACE INTO ", tablename, allColumns);
            insertOrReplaceStatement = db.compileStatement(sql);
        }
        return insertOrReplaceStatement;
    }

    public SQLiteStatement getDeleteStatement() {
        if (deleteStatement == null) {
            String sql = SqlUtils.createSqlDelete(tablename, idColumns);
            deleteStatement = db.compileStatement(sql);
        }
        return deleteStatement;
    }

    public String getNextSliceStatement(boolean asc) {
        String statement;
        if(asc) {
            statement = nextSliceStatementAsc;
        } else {
            statement = nextSliceStatementDesc;
        }

        if (statement == null) {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(tablename);
            sql.append(" WHERE ");
            sql.append(listEngineId).append("=? ");
            sql.append(" ORDER BY ").append(sortKey);
            if(asc) {
                sql.append(" ASC");
            } else {
                sql.append(" DESC");
            }
            sql.append(" LIMIT ?");
            sql.append(" OFFSET ?");

            statement = sql.toString();
            if(asc) {
                nextSliceStatementAsc = statement;
            } else {
                nextSliceStatementDesc = statement;
            }
        }
        return statement;
    }

    public String getGetByIdStatement() {
        if(getByIdStatement == null) {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(tablename);
            sql.append(" WHERE ");
            sql.append(listEngineId).append("=? AND ");
            sql.append(id).append("=? ");
            getByIdStatement = sql.toString();
        }
        return getByIdStatement;
    }

    public String getAllStatement() {
        if(allStatement == null) {
            StringBuilder sql = new StringBuilder("SELECT * FROM ");
            sql.append(tablename);
            sql.append(" WHERE ");
            sql.append(listEngineId).append("=?");
            allStatement = sql.toString();
        }
        return allStatement;
    }
}

