package com.xncoder.advanceprotection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class SaveSecureCode extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "secure_code.db";
    private static final int DATABASE_VERSION = 1;

    public SaveSecureCode(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public void insertData(String data) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("data", data);
        db.insert("mytable", null, values);
        db.close();
    }

    public String getData() {
        String data = null;
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM mytable", null);
        if (cursor.moveToFirst()) {
            data = cursor.getString(cursor.getColumnIndex("data"));
        }
        cursor.close();
        db.close();
        return data;
    }

    public void deleteData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete("mytable", null, null);
        db.close();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE mytable (id INTEGER PRIMARY KEY, data TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS mytable");
        onCreate(db);
    }
}
