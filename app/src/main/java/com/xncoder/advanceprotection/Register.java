package com.xncoder.advanceprotection;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Register extends AppCompatActivity {

    private FirebaseAuth auth;
    private Popup popupWindow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        TextView signinText = (TextView) findViewById(R.id.signinText);
        signinText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        auth = FirebaseAuth.getInstance();
        Button register = (Button) findViewById(R.id.registerButton);
        EditText emailText = (EditText) findViewById(R.id.reg_email);
        EditText passwordText = (EditText) findViewById(R.id.reg_password);
        EditText cpasswordText = (EditText) findViewById(R.id.reg_confirmpassword);
        register.setOnClickListener(view -> {
            String email, password, cpassword, popup_msg = "Please enter your ";
            boolean popshow = false;
            email = String.valueOf(emailText.getText());
            password = String.valueOf(passwordText.getText());
            cpassword = String.valueOf(cpasswordText.getText());
            if(TextUtils.isEmpty(email)) {
                popup_msg += "Email ";
                popshow = true;
            }
            if(TextUtils.isEmpty(password)) {
                popup_msg += "Password ";
                popshow = true;
            }
            if(TextUtils.isEmpty(cpassword)) {
                popup_msg += "Confirm password";
                popshow = true;
            }
            if(popshow) {
                popupWindow = new Popup(getApplicationContext(), false, popup_msg);
                popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
            } else if(!password.equals(cpassword)) {
                popupWindow = new Popup(getApplicationContext(), false, "Both passwords are not matched");
                popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
            } else {
                auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Intent verifyIntent = new Intent(Register.this, Verify.class);
                            FirebaseUser user = auth.getCurrentUser();
                            verifyIntent.putExtra("Email", email);
                            verifyIntent.putExtra("User", user);
                            startActivity(verifyIntent);
//                            popupWindow = new Popup(getApplicationContext(), true, "Registration Successfully");
//                            popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
//                            popupWindow.onBtn.setOnClickListener(view1 -> finish());
                        } else {
                            popupWindow = new Popup(getApplicationContext(), false, "Authentication failed : " + Objects.requireNonNull(task.getException()).getMessage());
                            popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
                        }
                    });
            }
        });
    }
}