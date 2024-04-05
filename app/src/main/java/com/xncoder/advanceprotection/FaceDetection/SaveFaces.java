package com.xncoder.advanceprotection.FaceDetection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SaveFaces extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "faces_database";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_FACES = "faces";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_FACE_VECTOR = "face_vector";

    public SaveFaces(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE " + TABLE_FACES + " (" +
                COLUMN_NAME + " TEXT," +
                COLUMN_FACE_VECTOR + " TEXT" + // You can also use BLOB instead of TEXT
                ")";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades if needed
    }

    public void addFace(String name, float[] faceVector) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_NAME, name);
        values.put(COLUMN_FACE_VECTOR, convertFloatArrayToString(faceVector)); // Convert float array to string
        db.insert(TABLE_FACES, null, values);
        db.close();
    }

    public List<Float> getFaceVector(String name) {
        List<Float> faceVector = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_FACES, new String[]{COLUMN_FACE_VECTOR}, COLUMN_NAME + "=?",
                new String[]{name}, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            String faceVectorString = cursor.getString(cursor.getColumnIndex(COLUMN_FACE_VECTOR));
            faceVector = convertStringToFloatArray(faceVectorString); // Convert string to float array
            cursor.close();
        }
        db.close();
        return faceVector;
    }

    public HashMap<String, Float[]> getAllData() {
        HashMap<String, Float[]> dataMap = new HashMap<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_FACES, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndex(COLUMN_NAME));
                String faceVectorString = cursor.getString(cursor.getColumnIndex(COLUMN_FACE_VECTOR));
                Float[] faceVector = convertStringToFloatArray(faceVectorString).toArray(new Float[0]);
                dataMap.put(name, faceVector);
            } while (cursor.moveToNext());
            cursor.close();
        }
        db.close();
        return dataMap;
    }

    public boolean deleteEntry(String name) {
        SQLiteDatabase db = this.getWritableDatabase();
        int deletedRows = db.delete(TABLE_FACES, COLUMN_NAME + "=?", new String[]{name});
        db.close();
        return deletedRows > 0;
    }

    public void deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_FACES, null, null);
        db.execSQL("DELETE FROM " + TABLE_FACES);
        db.close();
    }

    private String convertFloatArrayToString(float[] array) {
        StringBuilder stringBuilder = new StringBuilder();
        for (float value : array) {
            stringBuilder.append(value).append(",");
        }
        return stringBuilder.toString();
    }

    private List<Float> convertStringToFloatArray(String stringValue) {
        List<Float> floatList = new ArrayList<>();
        String[] stringArray = stringValue.split(",");
        for (String value : stringArray) {
            floatList.add(Float.parseFloat(value));
        }
        return floatList;
    }
}
