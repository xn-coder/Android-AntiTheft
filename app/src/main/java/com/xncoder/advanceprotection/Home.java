package com.xncoder.advanceprotection;

import android.Manifest;
import android.app.ActivityManager;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.LinearLayout;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.jetbrains.annotations.Nullable;

public class Home extends AppCompatActivity {

    private Button startAction;
    private Switch dnd, sys, read_sms, send_sms, code, camera, location;
    private final int REQUEST_CODE_WRITE_SETTINGS = 1;
    private final int REQUEST_CODE_DND = 2;
    private final int REQUEST_CODE_SMS_READ = 3;
    private final int REQUEST_CODE_SMS_SEND = 3;
    private final int REQUEST_CODE_CAMERA = 4;
    private final int REQUEST_CODE_GPS_LOCATION = 5;
    private NotificationManager notificationManager;
    private boolean isProcessStart;
    private SaveSecureCode saveSecureCode;

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

        isProcessStart = isServiceRunning(this, SmsService.class);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        dnd = findViewById(R.id.dnd_switch);
        LinearLayout dnd_lay = findViewById(R.id.dnd_layout);
        dnd.setOnClickListener(view -> dndPermission());
        dnd_lay.setOnClickListener(view -> dndPermission());
        if(notificationManager.isNotificationPolicyAccessGranted())
            dnd.setChecked(true);

        sys = findViewById(R.id.sys_switch);
        LinearLayout sys_lay = findViewById(R.id.sys_layout);
        sys.setOnClickListener(view -> sysPermission());
        sys_lay.setOnClickListener(view -> sysPermission());
        if(Settings.System.canWrite(this))
            sys.setChecked(true);

        read_sms = findViewById(R.id.sms_switch);
        LinearLayout sms_lay = findViewById(R.id.sms_layout);
        read_sms.setOnClickListener(view -> smsReadPermission());
        sms_lay.setOnClickListener(view -> smsReadPermission());
        if(checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
            read_sms.setChecked(true);

        send_sms = findViewById(R.id.sms_send_switch);
        LinearLayout sms_send_lay = findViewById(R.id.sms_send_layout);
        send_sms.setOnClickListener(view -> smsSendPermission());
        sms_lay.setOnClickListener(view -> smsSendPermission());
        if(checkSelfPermission(android.Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED)
            send_sms.setChecked(true);

        camera = findViewById(R.id.camera_switch);
        LinearLayout camera_lay = findViewById(R.id.camera_layout);
        camera.setOnClickListener(view -> cameraPermission());
        camera_lay.setOnClickListener(view -> cameraPermission());
        if(checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
            camera.setChecked(true);

        location = findViewById(R.id.location_switch);
        LinearLayout location_lay = findViewById(R.id.location_layout);
        location.setOnClickListener(view -> getGPSPermission());
        location_lay.setOnClickListener(view -> getGPSPermission());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
            location.setChecked(true);

        saveSecureCode = new SaveSecureCode(this);
        code = findViewById(R.id.code_switch);
        LinearLayout code_lay = findViewById(R.id.code_layout);
        code.setOnClickListener(view -> secureCode());
        code_lay.setOnClickListener(view -> secureCode());
        code.setChecked(saveSecureCode.getData()!=null);

        startAction = findViewById(R.id.startAction);
        startAction.setOnClickListener(view -> {
            if(dnd.isChecked() && sys.isChecked() && read_sms.isChecked() && send_sms.isChecked() && camera.isChecked() && location.isChecked()) {
                startProcess();
            } else if (!dnd.isChecked()) {
                Toast.makeText(this, "Please enable the DND mode permission", Toast.LENGTH_SHORT).show();
            } else if (!sys.isChecked()) {
                Toast.makeText(this, "Please enable the system setting permission", Toast.LENGTH_SHORT).show();
            } else if (!read_sms.isChecked()) {
                Toast.makeText(this, "Please enable the read sms permission", Toast.LENGTH_SHORT).show();
            } else if (!send_sms.isChecked()) {
                Toast.makeText(this, "Please enable the read sms permission", Toast.LENGTH_SHORT).show();
            } else if (!camera.isChecked()) {
                Toast.makeText(this, "Please enable the send sms permission", Toast.LENGTH_SHORT).show();
            } else if (!location.isChecked()) {
                Toast.makeText(this, "Please enable the get location permission", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private void startProcess() {
        Intent processIntent = new Intent(this, SmsService.class);
        if (isProcessStart) {
            stopService(processIntent);
            startAction.setText("Start");
            Toast.makeText(this, "Stoped", Toast.LENGTH_SHORT).show();
            isProcessStart = false;
        } else {
            Toast.makeText(this, "Starting...", Toast.LENGTH_SHORT).show();
            startAction.setText("Stop");
            startService(processIntent);
            isProcessStart = true;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        code.setChecked(saveSecureCode.getData()!=null);
        isProcessStart = isServiceRunning(this, SmsService.class);
        if (!isProcessStart) {
            startAction.setText("Start");
        } else {
            startAction.setText("Stop");
        }
    }

    @Override
    protected void onDestroy() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction("restartservice");
        broadcastIntent.setClass(this, SmsReceiver.class);
        this.sendBroadcast(broadcastIntent);
        super.onDestroy();
    }

    private void secureCode() {
        Intent codeIntent = new Intent(this, SecureCode.class);
        startActivity(codeIntent);
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
        if (!notificationManager.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivityForResult(intent, REQUEST_CODE_DND);
        } else {
            dnd.setChecked(true);
        }
    }

    private void smsReadPermission() {
        if (checkSelfPermission(Manifest.permission.RECEIVE_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECEIVE_SMS}, REQUEST_CODE_SMS_READ);
        } else {
            read_sms.setChecked(true);
        }
    }

    private void smsSendPermission() {
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_SMS_SEND);
        } else {
            send_sms.setChecked(true);
        }
    }

    private void cameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA);
        } else {
            camera.setChecked(true);
        }
    }

    private void getGPSPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION}, REQUEST_CODE_GPS_LOCATION);
        } else {
            location.setChecked(true);
        }
    }

    private void showSMSReadPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs to access your read sms permission to function properly.");
        builder.setPositiveButton("OK", (dialog, which) -> requestPermissions(new String[]{android.Manifest.permission.RECEIVE_SMS}, REQUEST_CODE_SMS_READ));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSMSReadPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your read sms. You can grant the permission in app settings.");
        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSMSSendPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs to access your send sms permission to function properly.");
        builder.setPositiveButton("OK", (dialog, which) -> requestPermissions(new String[]{Manifest.permission.SEND_SMS}, REQUEST_CODE_SMS_SEND));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showSMSSendPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your send sms. You can grant the permission in app settings.");
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
        builder.setMessage("This app needs to access your send sms permission to function properly.");
        builder.setPositiveButton("OK", (dialog, which) -> requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_CAMERA));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showCameraPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your send sms. You can grant the permission in app settings.");
        builder.setPositiveButton("Go to Settings", (dialog, which) -> {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", getPackageName(), null));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLocationPermission() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs to access your location permission to function properly.");
        builder.setPositiveButton("OK", (dialog, which) -> ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_CODE_GPS_LOCATION));
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void showLocationPermissionSettingsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Permission Required");
        builder.setMessage("This app needs access to your location. You can grant the permission in app settings.");
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
        if(requestCode == REQUEST_CODE_SMS_READ) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                read_sms.setChecked(true);
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECEIVE_SMS)) {
                showSMSReadPermission();
            } else {
                showSMSReadPermissionSettingsDialog();
            }
        }
        if(requestCode == REQUEST_CODE_SMS_SEND) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                send_sms.setChecked(true);
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                showSMSSendPermission();
            } else {
                showSMSSendPermissionSettingsDialog();
            }
        }
        if(requestCode == REQUEST_CODE_CAMERA) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                camera.setChecked(true);
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                showCameraPermission();
            } else {
                showCameraPermissionSettingsDialog();
            }
        }
        if (requestCode == REQUEST_CODE_GPS_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                location.setChecked(true);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", this.getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                showLocationPermission();
            } else {
                showLocationPermissionSettingsDialog();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_WRITE_SETTINGS) {
            sys.setChecked(Settings.System.canWrite(this));
        } else if (requestCode == REQUEST_CODE_DND) {
            dnd.setChecked(notificationManager.isNotificationPolicyAccessGranted());
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
            SaveCredentials credentials = new SaveCredentials(this);
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