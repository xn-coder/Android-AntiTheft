package com.xncoder.advanceprotection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SaveLocation extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "LocationLinks.db";
    private static final int DATABASE_VERSION = 1;
    public static final String TABLE_NAME = "locations";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_MAP_LINK = "map_link";

    private static final String TABLE_CREATE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_MAP_LINK + " TEXT NOT NULL);";

    public SaveLocation(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }

    public void updateLocation(String mapLink) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_MAP_LINK, mapLink);

        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_ID}, null, null, null, null, null);
        if (cursor.moveToFirst()) {
            db.update(TABLE_NAME, values, COLUMN_ID + " = ?", new String[]{String.valueOf(cursor.getInt(0))});
        } else {
            db.insert(TABLE_NAME, null, values);
        }
        cursor.close();
        db.close();
    }

    public String getLocationLink() {
        SQLiteDatabase db = this.getReadableDatabase();
        String mapLink = null;
        Cursor cursor = db.query(TABLE_NAME, new String[]{COLUMN_MAP_LINK},
                null, null, null, null,
                COLUMN_ID + " DESC", "1");

        if (cursor.moveToFirst()) {
            mapLink = cursor.getString(cursor.getColumnIndex(COLUMN_MAP_LINK));
        }
        cursor.close();
        db.close();
        return mapLink;
    }
}
