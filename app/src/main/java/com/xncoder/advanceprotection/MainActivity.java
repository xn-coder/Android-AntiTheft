package com.xncoder.advanceprotection;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent homeIntent = new Intent(MainActivity.this, Home.class);
        if (new SaveCredentials(this).getAllUsers().get(0) != null)
            startActivity(homeIntent);
        Button get_start = findViewById(R.id.getStartButton);
        get_start.setOnClickListener(view -> {
            Intent getStartIntent = new Intent(MainActivity.this, Login.class);
            startActivity(getStartIntent);
        });

    }
}