package com.xncoder.advanceprotection;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.media.Image;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.Consumer;

public class CustomAdapter extends BaseAdapter {
    private Context context;
    private List<CustomItems> contacts;
    private HashMap<String, CustomItems> selected;

    public CustomAdapter(Context context, List<CustomItems> contacts, List<CustomItems> selectedContacts) {
        this.context = context;
        this.contacts = contacts;
        this.selected = new HashMap<>();
        for (CustomItems selectedContact : selectedContacts) {
            this.selected.put(selectedContact.getName(), selectedContact);
        }
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
            convertView = inflater.inflate(R.layout.custom_list_item, null);
        }

        TextView nameTextView = convertView.findViewById(R.id.text_view_title);
        TextView numberTextView = convertView.findViewById(R.id.text_view_description);
        ImageView iconImage = convertView.findViewById(R.id.contact_select);

        CustomItems contact = contacts.get(position);
        nameTextView.setText(contact.getName());
        numberTextView.setText(contact.getNumber());
        iconImage.setVisibility(contact.getSelected() ? View.VISIBLE : View.GONE);

        convertView.setOnClickListener(view -> {
            if (selected.size() < 5 && view.findViewById(R.id.contact_select).getVisibility() == View.GONE) {
                contact.setSelected(true);
                selected.put(contact.getName(), contact);
                view.findViewById(R.id.contact_select).setVisibility(View.VISIBLE);
            } else if(view.findViewById(R.id.contact_select).getVisibility() == View.VISIBLE){
                contact.setSelected(false);
                selected.remove(contact.getName());
                view.findViewById(R.id.contact_select).setVisibility(View.GONE);
            } else {
                Toast.makeText(context, "You can select\nmaximum 5 contacts", Toast.LENGTH_SHORT).show();
            }
        });

        return convertView;
    }

    public HashMap<String, CustomItems> getContact() {
        return selected;
    }
}
