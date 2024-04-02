package com.xncoder.advanceprotection;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

public class CustomContactAdapter extends BaseAdapter {
    private Context context;
    private List<CustomItems> contacts;
    private SaveContacts saveContacts;
    private Database database;

    public CustomContactAdapter(Context context, List<CustomItems> contacts) {
        this.context = context;
        this.contacts = contacts;
        saveContacts = new SaveContacts(context);
        database = new Database(context);
    }

    @Override
    public int getCount() {
        return contacts.size();
    }

    @Override
    public Object getItem(int position) {
        return contacts.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.contact_list_item, null);
        }

        TextView nameTextView = convertView.findViewById(R.id.list_name);
        TextView numberTextView = convertView.findViewById(R.id.list_number);
        ImageButton removeItem = convertView.findViewById(R.id.contact_select);

        CustomItems contact = contacts.get(position);
        nameTextView.setText(contact.getName());
        numberTextView.setText(contact.getNumber());
        removeItem.setOnClickListener(view -> {
            if (saveContacts.deleteData(contacts.get(position).getNumber())) {
                database.deleteUserData(new SaveCredentials(context).getAllUsers().get(0).replace(".", "_"), contacts.get(position).getNumber());
                Toast.makeText(context, contacts.get(position).getNumber() + "\nremoved", Toast.LENGTH_SHORT).show();
                contacts.remove(position);
            }
            else
                Toast.makeText(context, contacts.get(position).getNumber()+"\nfailed", Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        });

        return convertView;
    }
}
