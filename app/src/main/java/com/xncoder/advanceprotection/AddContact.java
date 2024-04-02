package com.xncoder.advanceprotection;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class AddContact extends AppCompatActivity {

    private List<CustomItems> contactsList;
    private ImageButton selectContact;
    private PopupWindow popupWindow;
    private List<CustomItems> selectedContacts;
    private ArrayList<String> selected;
    private ListView contactListView;
    private SaveContacts saveContacts;
    private Database database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_contact);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        selectedContacts = new ArrayList<>();
        database = new Database(this);
        selected = new ArrayList<>();
        contactListView = findViewById(R.id.selected_contacts);
        saveContacts = new SaveContacts(this);
        Cursor cursor = saveContacts.getAllData();
        if(cursor.getCount() != 0) {
            while (cursor.moveToNext()) {
                selectedContacts.add(new CustomItems(cursor.getString(1), cursor.getString(2), true));
                selected.add(cursor.getString(2));
            }
            updateContactList();
        }
        cursor.close();

        contactsList = getContacts();
        Collections.sort(contactsList, Comparator.comparing(CustomItems::getName));
        selectContact = findViewById(R.id.select_contact);
        selectContact.setOnClickListener(view -> showPopup());
    }

    private List<CustomItems> getContacts() {
        List<CustomItems> contactsList = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER},
                null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                boolean slct = false;
                String contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                if (selected.contains(phoneNumber))
                    slct = true;
                contactsList.add(new CustomItems(contactName, phoneNumber, slct));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return contactsList;
    }

    private void showPopup() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.contact_popup, null);

        ListView contactList = popupView.findViewById(R.id.list_view);
        Button okButton = popupView.findViewById(R.id.button_ok);
        Button cancelButton = popupView.findViewById(R.id.button_cancel);

        CustomAdapter adapter = new CustomAdapter(this, contactsList, selectedContacts);
        contactList.setAdapter(adapter);

        popupWindow = new PopupWindow(popupView, (int) (getResources().getDisplayMetrics().widthPixels * 0.9),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.7));

        okButton.setOnClickListener(v -> {
            selectedContacts.clear();
            saveContacts.deleteAllData();
            database.deleteUserAllData(new SaveCredentials(this).getAllUsers().get(0).replace(".", "_"));
            adapter.getContact().forEach((s, customItems) -> {
                selectedContacts.add(customItems);
                saveContacts.insertData(customItems.getName(), customItems.getNumber());
                database.setData(new SaveCredentials(this).getAllUsers().get(0).replace(".", "_"), customItems.getNumber(), customItems.getName());
            });
            updateContactList();
            popupWindow.dismiss();
        });

        cancelButton.setOnClickListener(v -> {
            popupWindow.dismiss();
        });
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
    }

    private void updateContactList() {
        if(selectedContacts != null) {
            CustomContactAdapter adapterContact = new CustomContactAdapter(this, selectedContacts);
            contactListView.setAdapter(adapterContact);
        }
    }

}