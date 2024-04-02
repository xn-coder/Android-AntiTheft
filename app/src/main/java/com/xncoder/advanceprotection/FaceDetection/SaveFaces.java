package com.xncoder.advanceprotection.FaceDetection;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.RectF;
import android.util.Pair;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

public class SaveFaces extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "image_database";
    private static final int DATABASE_VERSION = 1;

    public SaveFaces(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableQuery = "CREATE TABLE images (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "image BLOB," +
                "label TEXT)";
        db.execSQL(createTableQuery);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int i, int i1) {
        db.execSQL("DROP TABLE IF EXISTS images");
        onCreate(db);
    }

    public long insertImage(Bitmap image, String label) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("image", getBytes(image));
        values.put("label", label);
        long id = db.insert("images", null, values);
        db.close();
        return id;
    }

    public List<Pair<Bitmap, String>> getAllImages() {
        List<Pair<Bitmap, String>> imageList = new ArrayList<>();
        String selectQuery = "SELECT * FROM images";
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                byte[] imageData = cursor.getBlob(cursor.getColumnIndex("image"));
                Bitmap image = getImage(imageData);
                String label = cursor.getString(cursor.getColumnIndex("label"));
                imageList.add(new Pair<>(image, label));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return imageList;
    }

    private byte[] getBytes(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
        return stream.toByteArray();
    }

    private Bitmap getImage(byte[] image) {
        return BitmapFactory.decodeByteArray(image, 0, image.length);
    }

}
