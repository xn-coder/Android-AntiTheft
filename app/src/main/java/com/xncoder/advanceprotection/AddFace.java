package com.xncoder.advanceprotection;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.xncoder.advanceprotection.FaceDetection.FaceRecognitionActivity;
import com.xncoder.advanceprotection.FaceDetection.SaveFaces;

import java.util.ArrayList;
import java.util.List;

public class AddFace extends AppCompatActivity {

    private final int REQUEST_CODE = 101;
    private SaveFaces saveFaces;
    private ListView facesListView;
    private List<CustomItems> listFaces;
    private Database database;
    private int faceNo = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_face);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        database = new Database(this);
        listFaces = new ArrayList<>();
        facesListView = findViewById(R.id.selected_faces);
        saveFaces = new SaveFaces(this);
        saveFaces.getAllData().forEach((name, vector) -> {
            listFaces.add(new CustomItems("Face " + faceNo, name, true));
            faceNo += 1;
        });
        updateFaceList();

        ImageButton addFace = findViewById(R.id.add_face_btn);
        addFace.setOnClickListener(view -> {
            if (faceNo != 5)
                startActivityForResult(new Intent(this, FaceRecognitionActivity.class), REQUEST_CODE);
            else
                Toast.makeText(this, "You can add maximum 5 faces", Toast.LENGTH_SHORT).show();
        });
    }

    private void showPopup(Bitmap tempBitmap, float[] tempVector) {

        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.image_popup, null);

        ImageView face = popupView.findViewById(R.id.dlg_image);
        EditText faceN = popupView.findViewById(R.id.face_input);
        Button okBtn = popupView.findViewById(R.id.button_ok);
        Button cancelBtn = popupView.findViewById(R.id.button_cancel);

        face.setImageBitmap(tempBitmap);

        PopupWindow popupWindow = new PopupWindow(popupView, (int) (getResources().getDisplayMetrics().widthPixels * 0.7),
                (int) (getResources().getDisplayMetrics().heightPixels * 0.5));

        okBtn.setOnClickListener(viewNav -> {
            String input  = String.valueOf(faceN.getText());
            if (!input.isEmpty() && new SaveFaces(this).getFaceVector(input).isEmpty()) {
                database.setFaceData(new SaveCredentials(this).getAllUsers().get(0).replace(".", "_"), input, tempVector);
                listFaces.add(new CustomItems("Face " + faceNo, input, true));
                faceNo += 1;
                saveFaces.addFace(input, tempVector);
                popupWindow.dismiss();
                updateFaceList();
            } else if (!new SaveFaces(this).getFaceVector(input).isEmpty()) {
                Toast.makeText(this, "This name already exists", Toast.LENGTH_SHORT).show();
            }

        });

        cancelBtn.setOnClickListener(viewNav -> popupWindow.dismiss());

        popupWindow.setOutsideTouchable(false);
        popupWindow.setFocusable(true);
        popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            if (data != null) {
                showPopup(data.getParcelableExtra("Bitmap"), data.getFloatArrayExtra("Vector"));
            }
        }
    }

    private void updateFaceList() {
        if(listFaces != null) {
            CustomFaceAdapter adapterFace = new CustomFaceAdapter(this, listFaces);
            facesListView.setAdapter(adapterFace);
        }
    }
}