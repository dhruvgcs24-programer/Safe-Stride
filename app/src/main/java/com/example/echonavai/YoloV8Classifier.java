package com.example.echonavai;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.RectF;
import android.util.Log;

import org.tensorflow.lite.Interpreter;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class YoloV8Classifier {

    private static final String TAG = "YOLO_DEBUG";

    private static final int INPUT_SIZE = 320;
    private static final int PIXEL_SIZE = 3;
    private static final int BYTES_PER_CHANNEL = 4;

    private static final float CONFIDENCE_THRESHOLD = 0.3f;
    private static final float NMS_THRESHOLD = 0.45f;

    private final Interpreter interpreter;
    private final List<String> labels;

    public YoloV8Classifier(Context context) throws IOException {
        interpreter = new Interpreter(loadModelFile(context, "yolov8n.tflite"));
        labels = loadLabels(context, "labels.txt");
        Log.d(TAG, "Labels loaded: " + labels.size());
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fileDescriptor = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private List<String> loadLabels(Context context, String fileName) throws IOException {
        List<String> result = new ArrayList<>();
        InputStream is = context.getAssets().open(fileName);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        String line;
        while ((line = reader.readLine()) != null) {
            result.add(line.trim());
        }
        reader.close();
        return result;
    }

    public List<Detection> detect(Bitmap bitmap) {
        Log.d(TAG, "detect() called");
        Log.d(TAG, "Output shape: " + java.util.Arrays.toString(interpreter.getOutputTensor(0).shape()));

        Bitmap resized = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true);
        ByteBuffer input = convertBitmapToByteBuffer(resized);

        // YOLOv8 raw output shape: [1, 84, 2100]
        float[][][] output = new float[1][84][2100];
        interpreter.run(input, output);

        List<Detection> detections = new ArrayList<>();

        int numPredictions = 2100;
        int numClasses = 80;

        for (int i = 0; i < numPredictions; i++) {

            float cx = output[0][0][i];
            float cy = output[0][1][i];
            float w  = output[0][2][i];
            float h  = output[0][3][i];

            float maxClassScore = 0f;
            int classId = -1;

            for (int c = 0; c < numClasses; c++) {
                float classScore = output[0][4 + c][i];
                if (classScore > maxClassScore) {
                    maxClassScore = classScore;
                    classId = c;
                }
            }

            if (maxClassScore < CONFIDENCE_THRESHOLD) continue;
            if (classId < 0 || classId >= labels.size()) continue;

            String label = labels.get(classId);

            // YOLOv8 gives center-x, center-y, width, height in model input scale
            float left = (cx - w / 2f) * bitmap.getWidth() / INPUT_SIZE;
            float top = (cy - h / 2f) * bitmap.getHeight() / INPUT_SIZE;
            float right = (cx + w / 2f) * bitmap.getWidth() / INPUT_SIZE;
            float bottom = (cy + h / 2f) * bitmap.getHeight() / INPUT_SIZE;

            // Clamp values inside bitmap bounds
            left = Math.max(0, left);
            top = Math.max(0, top);
            right = Math.min(bitmap.getWidth(), right);
            bottom = Math.min(bitmap.getHeight(), bottom);

            if (right <= left || bottom <= top) continue;

            RectF rect = new RectF(left, top, right, bottom);
            detections.add(new Detection(label, maxClassScore, rect));

            Log.d(TAG, "Accepted detection -> label=" + label +
                    ", score=" + maxClassScore +
                    ", box=" + rect.toString());
        }

        return applyNMS(detections);
    }

    private List<Detection> applyNMS(List<Detection> detections) {
        List<Detection> result = new ArrayList<>();
        Collections.sort(detections, new Comparator<Detection>() {
            @Override
            public int compare(Detection o1, Detection o2) {
                return Float.compare(o2.getConfidence(), o1.getConfidence());
            }
        });

        boolean[] removed = new boolean[detections.size()];

        for (int i = 0; i < detections.size(); i++) {
            if (removed[i]) continue;

            Detection current = detections.get(i);
            result.add(current);

            for (int j = i + 1; j < detections.size(); j++) {
                if (removed[j]) continue;

                Detection other = detections.get(j);

                // Apply NMS only for same class
                if (!current.getLabel().equals(other.getLabel())) continue;

                if (calculateIoU(current.getBox(), other.getBox()) > NMS_THRESHOLD) {
                    removed[j] = true;
                }
            }
        }

        return result;
    }

    private float calculateIoU(RectF a, RectF b) {
        float left = Math.max(a.left, b.left);
        float top = Math.max(a.top, b.top);
        float right = Math.min(a.right, b.right);
        float bottom = Math.min(a.bottom, b.bottom);

        float intersectionWidth = Math.max(0, right - left);
        float intersectionHeight = Math.max(0, bottom - top);
        float intersectionArea = intersectionWidth * intersectionHeight;

        float areaA = (a.right - a.left) * (a.bottom - a.top);
        float areaB = (b.right - b.left) * (b.bottom - b.top);
        float unionArea = areaA + areaB - intersectionArea;

        if (unionArea <= 0) return 0f;
        return intersectionArea / unionArea;
    }

    private ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(
                1 * INPUT_SIZE * INPUT_SIZE * PIXEL_SIZE * BYTES_PER_CHANNEL
        );
        byteBuffer.order(ByteOrder.nativeOrder());

        int[] pixels = new int[INPUT_SIZE * INPUT_SIZE];
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE);

        int index = 0;
        for (int y = 0; y < INPUT_SIZE; y++) {
            for (int x = 0; x < INPUT_SIZE; x++) {
                int pixel = pixels[index++];

                float r = ((pixel >> 16) & 0xFF) / 255.0f;
                float g = ((pixel >> 8) & 0xFF) / 255.0f;
                float b = (pixel & 0xFF) / 255.0f;

                byteBuffer.putFloat(r);
                byteBuffer.putFloat(g);
                byteBuffer.putFloat(b);
            }
        }

        return byteBuffer;
    }

    public void close() {
        interpreter.close();
    }
}