package com.xncoder.advanceprotection;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SecureCode extends AppCompatActivity {

    private EditText editText;
    private Button cancel, edit, set;
    private SaveSecureCode saveSecureCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_secure_code);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        editText = findViewById(R.id.secure_code);
        cancel = findViewById(R.id.cancel_btn);
        edit = findViewById(R.id.edit_btn);
        set = findViewById(R.id.set_btn);
        saveSecureCode = new SaveSecureCode(this);
        String code = saveSecureCode.getData();
        if(code != null) {
            editText.setText(code);
            editText.setEnabled(false);
            cancel.setVisibility(View.GONE);
            edit.setVisibility(View.VISIBLE);
            set.setVisibility(View.GONE);
        } else {
            cancel.setVisibility(View.GONE);
            edit.setVisibility(View.GONE);
            set.setVisibility(View.VISIBLE);
        }

        set.setOnClickListener(view -> {
            if(String.valueOf(editText.getText()).isEmpty()) {
                Toast.makeText(this, "Please enter secure code", Toast.LENGTH_SHORT).show();
            } else if(String.valueOf(editText.getText()).length() < 8) {
                Toast.makeText(this, "Secure code length must be\natleast 8 characters", Toast.LENGTH_SHORT).show();
            } else {
                saveSecureCode.deleteData();
                saveSecureCode.insertData(String.valueOf(editText.getText()));
                Toast.makeText(this, "Secure Code Saved.", Toast.LENGTH_SHORT).show();
                editText.setEnabled(false);
                set.setVisibility(View.GONE);
                edit.setVisibility(View.VISIBLE);
                cancel.setVisibility(View.GONE);
                finish();
            }
        });

        edit.setOnClickListener(view -> {
            editText.setEnabled(true);
            editText.setFocusable(true);
            set.setVisibility(View.VISIBLE);
            cancel.setVisibility(View.VISIBLE);
            edit.setVisibility(View.GONE);
        });

        cancel.setOnClickListener(view -> {
            editText.setText(saveSecureCode.getData());
            editText.setEnabled(false);
            set.setVisibility(View.GONE);
            cancel.setVisibility(View.GONE);
            edit.setVisibility(View.VISIBLE);
            finish();
        });

    }
}