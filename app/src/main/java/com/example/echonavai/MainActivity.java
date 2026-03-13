package com.example.echonavai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private TTSHelper ttsHelper;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                boolean allGranted = true;
                for (Map.Entry<String, Boolean> entry : result.entrySet()) {
                    if (!Boolean.TRUE.equals(entry.getValue())) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    ttsHelper.speak("All permissions granted. Ready to start assistance.");
                } else {
                    ttsHelper.speak("Some permissions were denied. Camera access is required.");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ttsHelper = new TTSHelper(this);

        Button btnStart = findViewById(R.id.btnStart);

        new Handler(Looper.getMainLooper()).postDelayed(() ->
                ttsHelper.speak("Welcome to EchoNav A I"), 1000);

        btnStart.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                ttsHelper.speak("Starting assistance");
                startActivity(new Intent(MainActivity.this, CameraActivity.class));
            } else {
                requestPermissionsNow();
            }
        });
    }

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermissionsNow() {
        permissionLauncher.launch(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }
    }
}