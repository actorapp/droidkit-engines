package com.droidkit.engine.list.sqlite;


import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.droidkit.core.Logger;
import com.droidkit.engine.sqlite.SqlStatements;

public class ListEngineTableStatements implements SqlStatements {
    private final SQLiteDatabase db;
    private final String tablename;

    private SQLiteStatement insertStatement;
    private SQLiteStatement insertOrReplaceStatement;
    private SQLiteStatement updateStatement;
    private SQLiteStatement deleteStatement;

    private String nextSliceStatementAsc;
    private String nextSliceStatementDesc;
    private String getByIdStatement;
    private String allStatement;

    public ListEngineTableStatements(SQLiteDatabase db, String tablename) {
        this.db = db;
        this.tablename = tablename;
    }

    public SQLiteStatement getInsertStatement() {
        if (insertStatement == null) {
            String sql = String.format("INSERT INTO %s ('LIST_ID','ID','SORT_KEY','BYTES') VALUES (?,?,?,?)", tablename);
            insertStatement = db.compileStatement(sql);
        }
        return insertStatement;
    }

    public SQLiteStatement getInsertOrReplaceStatement() {
        if (insertOrReplaceStatement == null) {
            String sql = String.format("INSERT OR REPLACE INTO %s ('LIST_ID','ID','SORT_KEY','BYTES') VALUES (?,?,?,?)", tablename);
            insertOrReplaceStatement = db.compileStatement(sql);
        }
        return insertOrReplaceStatement;
    }

    public SQLiteStatement getDeleteStatement() {
        if (deleteStatement == null) {
            String sql = String.format("DELETE FROM %s WHERE %s.'LIST_ID'=? AND %s.'ID'=?", tablename, tablename, tablename);
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
            statement = String.format("SELECT * FROM %s WHERE LIST_ID=? ORDER BY SORT_KEY %s LIMIT ? OFFSET ?", tablename, (asc ? "ASC" : "DESC"));
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
            getByIdStatement = String.format("SELECT * FROM %s WHERE LIST_ID=? AND ID=?", tablename);
        }
        return getByIdStatement;
    }

    public String getAllStatement() {
        if(allStatement == null) {
            allStatement = String.format("SELECT * FROM %s WHERE LIST_ID=?", tablename);
        }
        return allStatement;
    }
}

