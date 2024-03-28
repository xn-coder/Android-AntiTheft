package com.xncoder.advanceprotection;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Objects;

public class Verify extends AppCompatActivity {
    private FirebaseUser user;
    private Popup popupWindow;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verify);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        Intent intent = getIntent();
        TextView emailText = (TextView) findViewById(R.id.verify_email);
        if(intent != null) {
            String email = intent.getStringExtra("Email");
            emailText.setText(email);
            user = intent.getParcelableExtra("User");
            assert user != null;
            sendLink(user);
        }

        Button resend = (Button) findViewById(R.id.resend);
        resend.setOnClickListener(view -> {
            if(user != null)
                sendLink(user);
        });

        Button goLogin = (Button) findViewById(R.id.verify_login);
        goLogin.setOnClickListener(view -> {
            Intent loginIntent = new Intent(Verify.this, Login.class);
            loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(loginIntent);
            finish();
        });
    }

    private void sendLink(FirebaseUser user) {
        user.sendEmailVerification()
            .addOnCompleteListener(task -> {
                if(!task.isSuccessful()) {
                    popupWindow = new Popup(getApplicationContext(), false, "Authentication failed : " + Objects.requireNonNull(task.getException()).getMessage());
                    popupWindow.showAtLocation(findViewById(android.R.id.content), Gravity.CENTER, 0, 0);
                }
            });
    }

}