package com.example.echonavai;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.os.SystemClock;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {
    private boolean isGuiding = false;
    private List<RouteStep> currentGuidanceSteps = new ArrayList<>();
    private int guidanceIndex = 0;
    private PreviewView previewView;
    private TextView txtStatus;
    private TTSHelper ttsHelper;
    private YoloV8Classifier classifier;
    private ExecutorService cameraExecutor;
    private RouteManager routeManager;

    private long lastSpokenTime = 0L;
    private static final long SPEAK_COOLDOWN_MS = 2500;

    private List<Detection> latestDetections = new ArrayList<>();

    private String lastMessageCandidate = "";
    private int sameMessageCount = 0;

    private boolean isScanning = false;
    private long lastRecordedStepTime = 0L;
    private static final long RECORD_STEP_INTERVAL_MS = 3000;

    private final List<RouteStep> currentRouteSteps = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        txtStatus = findViewById(R.id.txtStatus);
        Button btnDescribe = findViewById(R.id.btnSpeakTest);
        Button btnScanRoute = findViewById(R.id.btnScanRoute);
        Button btnStopSaveRoute = findViewById(R.id.btnStopSaveRoute);
        Button btnStartGuidance = findViewById(R.id.btnStartGuidance);

        btnStartGuidance.setOnClickListener(v -> showRouteSelectionDialog());
        ttsHelper = new TTSHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        routeManager = new RouteManager(this);

        try {
            classifier = new YoloV8Classifier(this);
        } catch (IOException e) {
            Toast.makeText(this, "Model load failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnDescribe.setOnClickListener(v -> {
            String scene = buildSceneDescription(latestDetections);
            txtStatus.setText(scene);
            ttsHelper.speak(scene);
        });

        btnScanRoute.setOnClickListener(v -> startRouteScan());

        btnStopSaveRoute.setOnClickListener(v -> stopAndSaveRoute());

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Camera permission not granted", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void showRouteSelectionDialog() {

        List<SavedRoute> routes = routeManager.getAllRoutes();

        if (routes.isEmpty()) {
            ttsHelper.speak("No saved routes available");
            return;
        }

        String[] routeNames = new String[routes.size()];

        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = routes.get(i).getDestinationName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Destination")
                .setItems(routeNames, (dialog, which) -> {
                    startGuidance(routes.get(which));
                })
                .show();
    }
    private void startGuidance(SavedRoute route) {

        currentGuidanceSteps = route.getSteps();
        guidanceIndex = 0;
        isGuiding = true;

        txtStatus.setText("Guiding to " + route.getDestinationName());
        ttsHelper.speak("Starting navigation to " + route.getDestinationName());
    }

    private void startRouteScan() {
        isScanning = true;
        currentRouteSteps.clear();
        lastRecordedStepTime = 0L;
        txtStatus.setText("Route scanning started");
        ttsHelper.speak("Route scanning started");
    }

    private void stopAndSaveRoute() {
        if (!isScanning) {
            ttsHelper.speak("No route scan is active");
            return;
        }

        isScanning = false;

        if (currentRouteSteps.isEmpty()) {
            ttsHelper.speak("No route steps recorded");
            txtStatus.setText("No route steps recorded");
            return;
        }

        showSaveRouteDialog();
    }

    private void showSaveRouteDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter destination name");

        new AlertDialog.Builder(this)
                .setTitle("Save Route")
                .setMessage("Enter destination name")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String destinationName = input.getText().toString().trim();

                    if (destinationName.isEmpty()) {
                        destinationName = "Saved Destination";
                    }

                    SavedRoute route = new SavedRoute(destinationName, new ArrayList<>(currentRouteSteps));
                    routeManager.saveRoute(route);

                    txtStatus.setText("Route saved for " + destinationName);
                    ttsHelper.speak("Route saved for " + destinationName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture =
                ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, this::analyzeImage);

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(
                        this,
                        cameraSelector,
                        preview,
                        imageAnalysis
                );

                txtStatus.setText("Camera and AI started");
                ttsHelper.speak("Camera and AI started");

            } catch (Exception e) {
                txtStatus.setText("Failed to start camera");
                Toast.makeText(this, "Camera error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    @OptIn(markerClass = ExperimentalGetImage.class)
    private void analyzeImage(ImageProxy imageProxy) {
        try {
            Image mediaImage = imageProxy.getImage();
            if (mediaImage == null) {
                imageProxy.close();
                return;
            }

            Bitmap bitmap = ImageUtils.imageProxyToBitmap(imageProxy);
            if (bitmap == null) {
                imageProxy.close();
                return;
            }

            Bitmap rotated = rotateBitmap(bitmap, imageProxy.getImageInfo().getRotationDegrees());
            boolean wallAhead = WallDetector.isWallAhead(rotated);
            List<Detection> detections = classifier.detect(rotated);

            runOnUiThread(() -> updateUiAndSpeak(detections,wallAhead));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
    }

    private void updateUiAndSpeak(List<Detection> detections,boolean wallAhead) {
        if (wallAhead) {

            txtStatus.setText("Wall ahead");

            long now = SystemClock.elapsedRealtime();

            if (now - lastSpokenTime > SPEAK_COOLDOWN_MS) {
                ttsHelper.speak("Wall ahead. Stop or turn.");
                lastSpokenTime = now;
            }

            return;
        }
        if (isGuiding) {
            guideUser();
        }
        List<Detection> filtered = filterUsefulDetections(detections);
        latestDetections = filtered;

        String message = buildGuidanceMessage(filtered);
        txtStatus.setText(message);

        if (isScanning) {
            recordRouteStepIfNeeded(filtered, message);
        }

        if (message.equals(lastMessageCandidate)) {
            sameMessageCount++;
        } else {
            lastMessageCandidate = message;
            sameMessageCount = 1;
        }

        long now = SystemClock.elapsedRealtime();
        if (sameMessageCount >= 2 && now - lastSpokenTime > SPEAK_COOLDOWN_MS) {
            ttsHelper.speak(message);
            lastSpokenTime = now;
        }
    }

    private void guideUser() {

        if (guidanceIndex >= currentGuidanceSteps.size()) {

            ttsHelper.speak("Destination reached");
            txtStatus.setText("Destination reached");

            isGuiding = false;
            return;
        }

        RouteStep step = currentGuidanceSteps.get(guidanceIndex);

        txtStatus.setText(step.getInstruction());

        long now = SystemClock.elapsedRealtime();

        if (now - lastSpokenTime > SPEAK_COOLDOWN_MS) {

            ttsHelper.speak(step.getInstruction());

            lastSpokenTime = now;

            guidanceIndex++;
        }
    }

    private void recordRouteStepIfNeeded(List<Detection> detections, String message) {
        long now = SystemClock.elapsedRealtime();

        if (now - lastRecordedStepTime < RECORD_STEP_INTERVAL_MS) {
            return;
        }

        String landmark = "clear path";

        if (detections != null && !detections.isEmpty()) {
            Detection largest = getLargestDetection(detections);
            if (largest != null) {
                landmark = userFriendlyLabel(largest.getLabel()) + " " + getPosition(largest.getBox());
            }
        }

        currentRouteSteps.add(new RouteStep(message, landmark, System.currentTimeMillis()));
        lastRecordedStepTime = now;
    }

    private List<Detection> filterUsefulDetections(List<Detection> detections) {
        List<Detection> filtered = new ArrayList<>();

        if (detections == null) return filtered;

        for (Detection d : detections) {
            if (isUsefulIndoorObject(d.getLabel())) {
                filtered.add(d);
            }
        }

        return filtered;
    }

    private boolean isUsefulIndoorObject(String label) {
        return label.equals("person") ||
                label.equals("chair") ||
                label.equals("couch") ||
                label.equals("dining table") ||
                label.equals("backpack") ||
                label.equals("cell phone");
    }

    private String getPosition(RectF box) {
        float screenWidth = previewView.getWidth();

        float leftBoundary = screenWidth * 0.30f;
        float rightBoundary = screenWidth * 0.70f;

        if (box.left < rightBoundary && box.right > leftBoundary) {
            return "ahead";
        }

        float centerX = box.centerX();

        if (centerX < leftBoundary) {
            return "left";
        } else if (centerX > rightBoundary) {
            return "right";
        } else {
            return "ahead";
        }
    }

    private String userFriendlyLabel(String label) {
        if (label.equals("dining table")) return "table";
        if (label.equals("cell phone")) return "phone";
        return label;
    }

    private Detection getLargestDetection(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) return null;

        Detection largest = detections.get(0);
        float maxArea = largest.getBox().width() * largest.getBox().height();

        for (Detection d : detections) {
            float area = d.getBox().width() * d.getBox().height();
            if (area > maxArea) {
                maxArea = area;
                largest = d;
            }
        }

        return largest;
    }

    private String buildGuidanceMessage(List<Detection> detections) {

        if (detections == null || detections.isEmpty()) {
            return "Path ahead looks clear.";
        }

        int leftCount = 0;
        int centerCount = 0;
        int rightCount = 0;

        for (Detection d : detections) {

            String position = getPosition(d.getBox());

            if (position.equals("left")) {
                leftCount++;
            }
            else if (position.equals("ahead")) {
                centerCount++;
            }
            else if (position.equals("right")) {
                rightCount++;
            }
        }

        // obstacle directly ahead
        if (centerCount > 0) {

            if (leftCount == 0 && rightCount == 0) {
                return "Obstacle ahead. Move slightly left or right.";
            }

            if (leftCount < rightCount) {
                return "Obstacle ahead. Move left.";
            }

            if (rightCount < leftCount) {
                return "Obstacle ahead. Move right.";
            }

            return "Obstacle ahead. Move carefully.";
        }

        // object only on left
        if (leftCount > 0 && centerCount == 0) {
            return "Obstacle on the left. Path ahead is clear.";
        }

        // object only on right
        if (rightCount > 0 && centerCount == 0) {
            return "Obstacle on the right. Path ahead is clear.";
        }

        return "Path ahead looks clear.";
    }

    private String buildSceneDescription(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return "No major object detected nearby.";
        }

        StringBuilder sb = new StringBuilder();
        int count = Math.min(detections.size(), 3);

        for (int i = 0; i < count; i++) {
            Detection d = detections.get(i);
            String label = userFriendlyLabel(d.getLabel());
            String position = getPosition(d.getBox());

            if (i > 0) sb.append(", ");
            sb.append(label).append(" ").append(position);
        }

        return sb.toString();
    }

    private Bitmap rotateBitmap(Bitmap source, int rotationDegrees) {
        if (rotationDegrees == 0) return source;

        Matrix matrix = new Matrix();
        matrix.postRotate(rotationDegrees);

        return Bitmap.createBitmap(
                source,
                0,
                0,
                source.getWidth(),
                source.getHeight(),
                matrix,
                true
        );
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (ttsHelper != null) {
            ttsHelper.shutdown();
        }

        if (classifier != null) {
            classifier.close();
        }

        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
    }
}