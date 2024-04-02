package com.xncoder.advanceprotection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;
import android.provider.Settings;
import android.net.Uri;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.xncoder.advanceprotection.FaceDetection.CameraActivity;

public class Home extends AppCompatActivity {

    private SaveCredentials credentials;
    private Switch contactSwitch;
    private final int PERMISSION_REQUEST_READ_CONTACTS = 1;
    private final int PERMISSION_REQUEST_CAMERA = 2;
    private Database database;
    private String emailID;
    private SaveContacts saveContacts;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        View contact = findViewById(R.id.contact_layout);
        contact.setOnClickListener(view -> contacts());

        View camera = findViewById(R.id.camera_layout);
        camera.setOnClickListener(view -> camera());

        saveContacts = new SaveContacts(this);
        database = new Database(this);
        emailID = new SaveCredentials(this).getAllUsers().get(0);
        if (emailID != null)
            database.getUserData(emailID.replace(".", "_"));

        contactSwitch = findViewById(R.id.contact_switch);
        contactSwitch.setOnClickListener(view -> {
            if (saveContacts.getAllData().getCount() == 0) {
                contacts();
            }
        });
        contactSwitch.setChecked(saveContacts.getAllData().getCount() != 0);

        Button startAction = findViewById(R.id.startAction);
        startAction.setOnClickListener(view -> {
            if(contactSwitch.isChecked()){
                Toast.makeText(this, "Initializing...", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please enable the add contact", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        contactSwitch.setChecked(saveContacts.getAllData().getCount() != 0);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        contactSwitch.setChecked(saveContacts.getAllData().getCount() != 0);
    }

    private void camera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA);
        }
    }

    private void startCamera() {
        Intent cameraIntent = new Intent(this, AddFace.class);
        startActivity(cameraIntent);
    }

    private void contacts() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            readContacts();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
        }
    }

    private void readContacts() {
        Intent addIntent = new Intent(Home.this, AddContact.class);
        startActivity(addIntent);
    }

    private void showContactPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your contacts to function properly.");
        builder.setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(Home.this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showContactPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your contacts. You can grant the permission in app settings.");
        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCameraPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your contacts to function properly.");
        builder.setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(Home.this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCameraPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your contacts. You can grant the permission in app settings.");
        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                readContacts();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                    showContactPermission();
                } else {
                    showContactPermissionSettingsDialog();
                }
            }
        }
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                    showCameraPermission();
                } else {
                    showCameraPermissionSettingsDialog();
                }
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.signoutBtn) {
            credentials = new SaveCredentials(this);
            credentials.clearDatabase();
            Intent loginIntent = new Intent(Home.this, Login.class);
            startActivity(loginIntent);
            finish();
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }
}