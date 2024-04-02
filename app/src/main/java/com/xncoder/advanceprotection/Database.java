package com.xncoder.advanceprotection;

import android.content.Context;
import android.util.Log;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class Database {
    private DatabaseReference db = FirebaseDatabase.getInstance().getReference();
    private Context context;

    public Database(Context context) {
        this.context = context;
    }

    public void setData(String userId, String key, String name) {
        DatabaseReference userRef = db.child("AllUsers").child(userId);
        userRef.child(key).setValue(name);
    }
    public void deleteUserData(String userId, String key) {
        DatabaseReference userRef = db.child("AllUsers").child(userId);
        DatabaseReference keyRef = userRef.child(key);
        keyRef.removeValue();
    }
    public void deleteUserAllData(String userId) {
        DatabaseReference userRef = db.child("AllUsers").child(userId);
        userRef.removeValue();
    }
    public void getUserData(String userId) {
        new SaveContacts(context).deleteAllData();
        DatabaseReference userRef = db.child("AllUsers").child(userId);
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
}