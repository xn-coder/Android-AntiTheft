package com.xncoder.advanceprotection;

import android.Manifest;
import android.app.NotificationManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.xncoder.advanceprotection.FaceDetection.SaveFaces;

public class Home extends AppCompatActivity {

    private SaveCredentials credentials;
    private Switch contactSwitch, faceSwitch, display, dnd, sys;
    private final int PERMISSION_REQUEST_READ_CONTACTS = 1;
    private final int PERMISSION_REQUEST_CAMERA = 2;
    private final int REQUEST_CODE_DRAW_OVERLAY_PERMISSION = 3;
    private final int REQUEST_CODE_WRITE_SETTINGS = 4;
    private final int REQUEST_CODE_DND = 5;
    private SaveContacts saveContacts;
    private SaveFaces saveFaces;
    private NotificationManager notificationManager;
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

        Database database = new Database(this);
        String emailID = new SaveCredentials(this).getAllUsers().get(0);
        if (emailID != null) {
            database.getFaceData(emailID.replace(".", "_"));
            database.getContactData(emailID.replace(".", "_"));
        }

        Toolbar myToolbar = findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);

        View contact = findViewById(R.id.contact_layout);
        contact.setOnClickListener(view -> contacts());

        View camera = findViewById(R.id.camera_layout);
        camera.setOnClickListener(view -> camera());

        saveContacts = new SaveContacts(this);
        contactSwitch = findViewById(R.id.contact_switch);
        contactSwitch.setOnClickListener(view -> contacts());
        contactSwitch.setChecked(saveContacts.getAllData().getCount() != 0);

        saveFaces = new SaveFaces(this);
        faceSwitch = findViewById(R.id.camera_switch);
        faceSwitch.setOnClickListener(view -> camera());
        faceSwitch.setChecked(!saveFaces.getAllData().isEmpty());

        display = findViewById(R.id.display_switch);
        display.setOnClickListener(view -> displayPermission());

        dnd = findViewById(R.id.dnd_switch);
        dnd.setOnClickListener(view -> dndPermission());

        sys = findViewById(R.id.sys_switch);
        sys.setOnClickListener(view -> sysPermission());

        Button startAction = findViewById(R.id.startAction);
        startAction.setOnClickListener(view -> {
            if(contactSwitch.isChecked() && faceSwitch.isChecked() && display.isChecked() && dnd.isChecked() && sys.isChecked()) {
                startProcess();
            } else if (!contactSwitch.isChecked()) {
                Toast.makeText(this, "Please enable the add contact", Toast.LENGTH_SHORT).show();
            } else if (!faceSwitch.isChecked()) {
                Toast.makeText(this, "Please enable the add face", Toast.LENGTH_SHORT).show();
            } else if (!display.isChecked()) {
                Toast.makeText(this, "Please enable the display permission", Toast.LENGTH_SHORT).show();
            } else if (!dnd.isChecked()) {
                Toast.makeText(this, "Please enable the DND mode permission", Toast.LENGTH_SHORT).show();
            } else if (!sys.isChecked()) {
                Toast.makeText(this, "Please enable the system setting permission", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPermission() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_DRAW_OVERLAY_PERMISSION);
        } else {
            display.setChecked(true);
        }
    }

    private void sysPermission() {
        if (!Settings.System.canWrite(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, REQUEST_CODE_WRITE_SETTINGS);
        } else {
            sys.setChecked(true);
        }
    }

    private void dndPermission() {
        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_DND);
        } else {
            dnd.setChecked(true);
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        contactSwitch.setChecked(saveContacts.getAllData().getCount() != 0);
        faceSwitch.setChecked(!saveFaces.getAllData().isEmpty());
    }

    @Override
    protected void onResume() {
        super.onResume();
        contactSwitch.setChecked(saveContacts.getAllData().getCount() != 0);
        faceSwitch.setChecked(!saveFaces.getAllData().isEmpty());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            if (Settings.System.canWrite(this)) {
                sys.setChecked(true);
            } else {
                sys.setChecked(false);
            }
        } else if (requestCode == REQUEST_CODE_DRAW_OVERLAY_PERMISSION) {
            if (Settings.canDrawOverlays(this)) {
                display.setChecked(true);
            } else {
                display.setChecked(false);
            }
        } else if (requestCode == REQUEST_CODE_DND) {
            if (notificationManager.isNotificationPolicyAccessGranted()) {
                dnd.setChecked(true);
            } else {
                dnd.setChecked(false);
            }
        }
    }

    private void startProcess() {
//            startActivity(new Intent(this, DummyPowerScreen.class));
//            Toast.makeText(this, "Initializing...", Toast.LENGTH_SHORT).show();
        startService(new Intent(this, StartService.class));
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
        builder.setMessage("This app needs access to your camera to function properly.");
        builder.setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, PERMISSION_REQUEST_CAMERA));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCameraPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your camera. You can grant the permission in app settings.");
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
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
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