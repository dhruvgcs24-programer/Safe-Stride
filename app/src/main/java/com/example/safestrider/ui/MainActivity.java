package com.example.safestrider.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.safestrider.R;
import com.example.safestrider.navigation.RouteManager;
import com.example.safestrider.navigation.SavedRoute;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 101;

    private Button btnStartNavigation;
    private Button btnScanRoute;
    private Button btnSavedRoutes;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStartNavigation = findViewById(R.id.btnStartNavigation);
        btnScanRoute = findViewById(R.id.btnScanRoute);
        btnSavedRoutes = findViewById(R.id.btnSavedRoutes);

        requestRequiredPermissions();

        btnStartNavigation.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra("mode", "NAVIGATION");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please allow camera permission first", Toast.LENGTH_SHORT).show();
                requestRequiredPermissions();
            }
        });

        btnScanRoute.setOnClickListener(v -> {
            if (hasAllPermissions()) {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                intent.putExtra("mode", "SCAN");
                startActivity(intent);
            } else {
                Toast.makeText(this, "Please allow camera permission first", Toast.LENGTH_SHORT).show();
                requestRequiredPermissions();
            }
        });

        btnSavedRoutes.setOnClickListener(v -> showSavedRoutesDialog());
    }

    private void showSavedRoutesDialog() {
        RouteManager routeManager = new RouteManager(this);
        List<SavedRoute> routes = routeManager.getAllRoutes();

        if (routes.isEmpty()) {
            Toast.makeText(this, "No saved routes found", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] routeNames = new String[routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = routes.get(i).getDestinationName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Saved Routes")
                .setItems(routeNames, (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                    intent.putExtra("mode", "GUIDE_SAVED");
                    intent.putExtra("route_name", routes.get(which).getDestinationName());
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void requestRequiredPermissions() {
        if (!hasAllPermissions()) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{Manifest.permission.CAMERA},
                    PERMISSION_REQUEST_CODE
            );
        }
    }

    private boolean hasAllPermissions() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (hasAllPermissions()) {
                Toast.makeText(this, "Camera permission granted", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Camera permission is required", Toast.LENGTH_LONG).show();
            }
        }
    }
}