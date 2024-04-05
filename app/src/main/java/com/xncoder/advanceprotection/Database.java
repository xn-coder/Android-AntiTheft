package com.xncoder.advanceprotection;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.xncoder.advanceprotection.FaceDetection.SaveFaces;

import java.util.Arrays;
import java.util.Objects;

public class Database {
    private DatabaseReference db = FirebaseDatabase.getInstance().getReference();
    private Context context;
    public Database(Context context) {
        this.context = context;
    }

    public void setContactData(String userId, String key, String name) {
        DatabaseReference userRef = db.child("AllUsers").child(userId).child("Contacts");
        userRef.child(key).setValue(name);
    }

    public void setFaceData(String userId, String name, float[] vector) {
        DatabaseReference userRef = db.child("AllUsers").child(userId).child("Faces");
        userRef.child(name).setValue(floatArrayToString(vector));
    }

    public String floatArrayToString(float[] array) {
        StringBuilder sb = new StringBuilder();
        for (float value : array) {
            sb.append(value).append(",");
        }
        if (sb.length() > 0) {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    public float[] stringToFloatArray(String str) {
        String[] parts = str.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i]);
        }
        return result;
    }

    public void deleteContactData(String userId, String key) {
        DatabaseReference userRef = db.child("AllUsers").child(userId).child("Contacts");
        DatabaseReference keyRef = userRef.child(key);
        keyRef.removeValue();
    }

    public void deleteFaceData(String userId, String name) {
        DatabaseReference userRef = db.child("AllUsers").child(userId).child("Faces");
        DatabaseReference keyRef = userRef.child(name);
        keyRef.removeValue();
    }

    public void deleteAllContactData(String userId) {
        DatabaseReference userRef = db.child("AllUsers").child(userId).child("Contacts");
        userRef.removeValue();
    }

    public void getContactData(String userId) {
        new SaveContacts(context).deleteAllData();
        DatabaseReference userRef = db.child("AllUsers").child(userId).child("Contacts");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    new SaveContacts(context).insertData(snapshot.getValue(String.class), snapshot.getKey());
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("getUserData", "Error: " + databaseError.getMessage());
            }
        });
    }

    public void getFaceData(String userId) {
        new SaveFaces(context).deleteAllData();
        DatabaseReference userRef = db.child("AllUsers").child(userId).child("Faces");
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    new SaveFaces(context).addFace(snapshot.getKey(), stringToFloatArray(snapshot.getValue(String.class)));
                }
            }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e("getUserData", "Error: " + databaseError.getMessage());
            }
        });
    }
}