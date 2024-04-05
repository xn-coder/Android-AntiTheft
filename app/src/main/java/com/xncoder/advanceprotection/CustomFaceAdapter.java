package com.xncoder.advanceprotection;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.xncoder.advanceprotection.FaceDetection.SaveFaces;

import java.util.List;

public class CustomFaceAdapter extends BaseAdapter {
    private Context context;
    private List<CustomItems> faces;
    private SaveFaces saveFaces;
    private Database database;

    public CustomFaceAdapter(Context context, List<CustomItems> faces) {
        this.context = context;
        this.faces = faces;
        saveFaces = new SaveFaces(context);
        database = new Database(context);
    }

    @Override
    public int getCount() {
        return faces.size();
    }

    @Override

    public Object getItem(int position) {
        return faces.get(position);
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

        CustomItems contact = faces.get(position);
        nameTextView.setText(contact.getName());
        numberTextView.setText(contact.getNumber());
        removeItem.setOnClickListener(view -> {
            if (saveFaces.deleteEntry(faces.get(position).getNumber())) {
                database.deleteFaceData(new SaveCredentials(context).getAllUsers().get(0).replace(".", "_"), faces.get(position).getNumber());
                Toast.makeText(context, faces.get(position).getNumber() + "\nremoved", Toast.LENGTH_SHORT).show();
                faces.remove(position);
            }
            else
                Toast.makeText(context, faces.get(position).getNumber()+"\nfailed", Toast.LENGTH_SHORT).show();
            notifyDataSetChanged();
        });

        return convertView;
    }
}
