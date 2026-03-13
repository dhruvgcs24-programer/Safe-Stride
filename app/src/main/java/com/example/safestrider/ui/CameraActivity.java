package com.example.safestrider.ui;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.media.Image;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
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

import com.example.safestrider.R;
import com.example.safestrider.navigation.NavigationController;
import com.example.safestrider.navigation.RouteManager;
import com.example.safestrider.navigation.RouteStep;
import com.example.safestrider.navigation.SavedRoute;
import com.example.safestrider.speech.TTSHelper;
import com.example.safestrider.utils.ImageUtils;
import com.example.safestrider.vision.Detection;
import com.example.safestrider.vision.DetectionOverlayView;
import com.example.safestrider.vision.DetectionTracker;
import com.example.safestrider.vision.IndoorObjectHelper;
import com.example.safestrider.vision.SafetyEvaluator;
import com.example.safestrider.vision.TrackedObstacle;
import com.example.safestrider.vision.WallDetector;
import com.example.safestrider.vision.YoloV8Classifier;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CameraActivity extends AppCompatActivity {

    private PreviewView previewView;
    private DetectionOverlayView detectionOverlay;
    private TextView txtStatus;

    private TTSHelper ttsHelper;
    private YoloV8Classifier classifier;
    private ExecutorService cameraExecutor;
    private RouteManager routeManager;
    private NavigationController navigationController;
    private DetectionTracker detectionTracker;

    private static final float MIN_CONFIDENCE = 0.45f;
    private static final long RECORD_STEP_INTERVAL_MS = 3000;
    private static final long STABLE_HAZARD_SPEAK_INTERVAL_MS = 5000;
    private static final long GUIDANCE_COOLDOWN_MS = 7000;

    private final List<RouteStep> currentRouteSteps = new ArrayList<>();
    private List<Detection> latestDetections = new ArrayList<>();

    private boolean isScanning = false;
    private long lastRecordedStepTime = 0L;

    private String lastSpokenMessage = "";
    private String lastStableHazardMessage = "";
    private long lastStableHazardSpeakTime = 0L;

    private String lastGuidanceMessage = "";
    private long lastGuidanceSpeakTime = 0L;

    private boolean isFlashOn = false;
    private CameraManager cameraManager;
    private String cameraId;

    private String launchMode = "NAVIGATION";
    private String selectedRouteName = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        previewView = findViewById(R.id.previewView);
        detectionOverlay = findViewById(R.id.detectionOverlay);
        txtStatus = findViewById(R.id.txtStatus);

        Button btnDescribe = findViewById(R.id.btnSpeakTest);
        Button btnScanRoute = findViewById(R.id.btnScanRoute);
        Button btnStopSaveRoute = findViewById(R.id.btnStopSaveRoute);
        Button btnStartGuidance = findViewById(R.id.btnStartGuidance);
        Button btnRepeatLast = findViewById(R.id.btnRepeatLast);
        Button btnToggleVoice = findViewById(R.id.btnToggleVoice);
        Button btnFlash = findViewById(R.id.btnFlash);
        Button btnClearRoutes = findViewById(R.id.btnClearRoutes);

        launchMode = getIntent().getStringExtra("mode");
        if (launchMode == null) {
            launchMode = "NAVIGATION";
        }
        selectedRouteName = getIntent().getStringExtra("route_name");

        ttsHelper = new TTSHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();
        routeManager = new RouteManager(this);
        navigationController = new NavigationController();
        detectionTracker = new DetectionTracker();

        cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            if (cameraManager != null) {
                cameraId = cameraManager.getCameraIdList()[0];
            }
        } catch (Exception e) {
            cameraId = null;
        }

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
            lastSpokenMessage = scene;
        });

        btnScanRoute.setOnClickListener(v -> startRouteScan());
        btnStopSaveRoute.setOnClickListener(v -> stopAndSaveRoute());
        btnStartGuidance.setOnClickListener(v -> showRouteSelectionDialog());

        btnRepeatLast.setOnClickListener(v -> {
            if (!lastSpokenMessage.isEmpty()) {
                txtStatus.setText(lastSpokenMessage);
                ttsHelper.speakPriority(lastSpokenMessage);
            }
        });

        btnToggleVoice.setOnClickListener(v -> {
            boolean newMutedState = !ttsHelper.isMuted();
            ttsHelper.setMuted(newMutedState);
            btnToggleVoice.setText(newMutedState ? "Unmute Voice" : "Mute Voice");
            txtStatus.setText(newMutedState ? "Voice muted" : "Voice enabled");
        });

        btnFlash.setOnClickListener(v -> toggleFlash(btnFlash));

        btnClearRoutes.setOnClickListener(v -> {
            routeManager.clearAllRoutes();
            txtStatus.setText("All saved routes cleared");
            ttsHelper.speakPriority("All saved routes cleared");
            lastSpokenMessage = "All saved routes cleared";
        });

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            startCamera();
            handleLaunchMode();
        } else {
            Toast.makeText(this, "Camera permission not granted", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void handleLaunchMode() {
        if ("SCAN".equalsIgnoreCase(launchMode)) {
            startRouteScan();
        } else if ("GUIDE_SAVED".equalsIgnoreCase(launchMode) && selectedRouteName != null) {
            SavedRoute savedRoute = routeManager.getRouteByName(selectedRouteName);
            if (savedRoute != null) {
                startGuidance(savedRoute);
            } else {
                txtStatus.setText("Saved route not found");
                ttsHelper.speakPriority("Saved route not found");
                lastSpokenMessage = "Saved route not found";
            }
        }
    }

    private void showRouteSelectionDialog() {
        List<SavedRoute> routes = routeManager.getAllRoutes();

        if (routes.isEmpty()) {
            ttsHelper.speakPriority("No saved routes available");
            lastSpokenMessage = "No saved routes available";
            return;
        }

        String[] routeNames = new String[routes.size()];
        for (int i = 0; i < routes.size(); i++) {
            routeNames[i] = routes.get(i).getDestinationName();
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Destination")
                .setItems(routeNames, (dialog, which) -> startGuidance(routes.get(which)))
                .show();
    }

    private void startGuidance(SavedRoute route) {
        navigationController.startGuidance(route);
        txtStatus.setText("Guiding to " + route.getDestinationName());
        ttsHelper.speakPriority("Starting navigation to " + route.getDestinationName());
        lastSpokenMessage = "Starting navigation to " + route.getDestinationName();
        lastGuidanceMessage = "";
        lastGuidanceSpeakTime = 0L;
    }

    private void startRouteScan() {
        isScanning = true;
        currentRouteSteps.clear();
        detectionTracker.clear();
        lastRecordedStepTime = 0L;
        txtStatus.setText("Route scanning started");
        ttsHelper.speakPriority("Route scanning started");
        lastSpokenMessage = "Route scanning started";
    }

    private void stopAndSaveRoute() {
        if (!isScanning) {
            ttsHelper.speakPriority("No route scan is active");
            lastSpokenMessage = "No route scan is active";
            return;
        }

        isScanning = false;

        if (currentRouteSteps.isEmpty()) {
            txtStatus.setText("No route steps recorded");
            ttsHelper.speakPriority("No route steps recorded");
            lastSpokenMessage = "No route steps recorded";
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
                    ttsHelper.speakPriority("Route saved for " + destinationName);
                    lastSpokenMessage = "Route saved for " + destinationName;
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
                        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
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
                txtStatus.setAlpha(0f);
                txtStatus.animate().alpha(1f).setDuration(800);
                ttsHelper.speakPriority("Camera and AI started");
                lastSpokenMessage = "Camera and AI started";

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
            List<Detection> filteredDetections = filterUsefulDetections(detections);

            runOnUiThread(() -> {
                detectionOverlay.setDetections(filteredDetections);
                updateUiAndSpeak(filteredDetections, wallAhead);
            });

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            imageProxy.close();
        }
    }

    private void updateUiAndSpeak(List<Detection> detections, boolean wallAhead) {
        if (wallAhead) {
            String wallMessage = "Wall ahead about 0.8 meters. Stop or turn.";
            txtStatus.setText(wallMessage);

            long now = SystemClock.elapsedRealtime();
            boolean shouldSpeakWall =
                    !wallMessage.equalsIgnoreCase(lastStableHazardMessage)
                            || (now - lastStableHazardSpeakTime > STABLE_HAZARD_SPEAK_INTERVAL_MS);

            if (shouldSpeakWall) {
                ttsHelper.speakPriority(wallMessage);
                vibrateWarning();
                lastSpokenMessage = wallMessage;
                lastStableHazardMessage = wallMessage;
                lastStableHazardSpeakTime = now;
            }
            return;
        }

        if (navigationController.isGuiding()) {
            guideUser();
        }

        latestDetections = detections;

        TrackedObstacle stableObstacle =
                detectionTracker.updateAndGetBestStableObstacle(detections, previewView.getWidth());

        String message = SafetyEvaluator.evaluatePath(
                detections,
                previewView.getWidth(),
                previewView.getHeight()
        );

        txtStatus.setText(message);

        if (isScanning) {
            recordRouteStepIfNeeded(detections, message);
        }

        if ("Path ahead looks clear.".equalsIgnoreCase(message)) {
            return;
        }

        // Only speak after obstacle becomes stable
        if (stableObstacle == null) {
            return;
        }

        long now = SystemClock.elapsedRealtime();
        boolean shouldSpeakHazard =
                !message.equalsIgnoreCase(lastStableHazardMessage)
                        || (now - lastStableHazardSpeakTime > STABLE_HAZARD_SPEAK_INTERVAL_MS);

        if (shouldSpeakHazard) {
            ttsHelper.speak(message);
            vibrateWarning();
            lastSpokenMessage = message;
            lastStableHazardMessage = message;
            lastStableHazardSpeakTime = now;
        }
    }

    private void guideUser() {
        String instruction = navigationController.getCurrentInstruction();

        if (instruction == null || instruction.isEmpty()) {
            return;
        }

        txtStatus.setText(instruction);

        long now = SystemClock.elapsedRealtime();
        boolean shouldSpeakGuidance =
                !instruction.equalsIgnoreCase(lastGuidanceMessage)
                        || (now - lastGuidanceSpeakTime > GUIDANCE_COOLDOWN_MS);

        if (!shouldSpeakGuidance) {
            return;
        }

        if ("Destination reached".equalsIgnoreCase(instruction)) {
            ttsHelper.speakPriority(instruction);
            lastSpokenMessage = instruction;
            lastGuidanceMessage = instruction;
            lastGuidanceSpeakTime = now;
            navigationController.stopGuidance();
            return;
        }

        ttsHelper.speak(instruction);
        lastSpokenMessage = instruction;
        lastGuidanceMessage = instruction;
        lastGuidanceSpeakTime = now;
        navigationController.moveToNextStep();
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
                landmark = IndoorObjectHelper.userFriendlyLabel(largest.getLabel()) + " "
                        + SafetyEvaluator.getPosition(largest.getBox(), previewView.getWidth());
            }
        }

        currentRouteSteps.add(new RouteStep(message, landmark, System.currentTimeMillis()));
        lastRecordedStepTime = now;
    }

    private List<Detection> filterUsefulDetections(List<Detection> detections) {
        List<Detection> filtered = new ArrayList<>();

        if (detections == null) {
            return filtered;
        }

        for (Detection detection : detections) {
            if (detection.getConfidence() < MIN_CONFIDENCE) {
                continue;
            }

            if (IndoorObjectHelper.isUsefulIndoorObject(detection.getLabel())) {
                filtered.add(detection);
            }
        }

        return filtered;
    }

    private Detection getLargestDetection(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return null;
        }

        Detection largest = detections.get(0);
        float maxArea = largest.getArea();

        for (Detection detection : detections) {
            float area = detection.getArea();
            if (area > maxArea) {
                maxArea = area;
                largest = detection;
            }
        }

        return largest;
    }

    private String buildSceneDescription(List<Detection> detections) {
        if (detections == null || detections.isEmpty()) {
            return "No major object detected nearby.";
        }

        StringBuilder sb = new StringBuilder();
        int count = Math.min(detections.size(), 3);

        for (int i = 0; i < count; i++) {
            Detection detection = detections.get(i);
            String label = IndoorObjectHelper.userFriendlyLabel(detection.getLabel());
            String position = SafetyEvaluator.getPosition(detection.getBox(), previewView.getWidth());
            float distance = SafetyEvaluator.estimateDistanceMeters(
                    detection,
                    previewView.getWidth(),
                    previewView.getHeight()
            );

            if (i > 0) {
                sb.append(", ");
            }

            sb.append(label)
                    .append(" ")
                    .append(position)
                    .append(" about ")
                    .append(String.format(java.util.Locale.US, "%.1f meters", distance));
        }

        return sb.toString();
    }

    private void toggleFlash(Button btnFlash) {
        if (cameraManager == null || cameraId == null) {
            txtStatus.setText("Flash not available");
            return;
        }

        try {
            isFlashOn = !isFlashOn;
            cameraManager.setTorchMode(cameraId, isFlashOn);
            btnFlash.setText(isFlashOn ? "Flash Off" : "Flash On");
            txtStatus.setText(isFlashOn ? "Flashlight enabled" : "Flashlight disabled");
        } catch (CameraAccessException e) {
            txtStatus.setText("Failed to control flash");
        }
    }

    private void vibrateWarning() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) {
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                    VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            );
        } else {
            vibrator.vibrate(200);
        }
    }

    private Bitmap rotateBitmap(Bitmap source, int rotationDegrees) {
        if (rotationDegrees == 0) {
            return source;
        }

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

        if (detectionTracker != null) {
            detectionTracker.clear();
        }

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