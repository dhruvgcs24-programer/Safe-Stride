package com.example.safestrider.vision;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DetectionOverlayView extends View {

    private final Paint boxPaint = new Paint();
    private final Paint textPaint = new Paint();
    private final Paint textBgPaint = new Paint();

    private List<Detection> detections = new ArrayList<>();

    public DetectionOverlayView(Context context) {
        super(context);
        init();
    }

    public DetectionOverlayView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public DetectionOverlayView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        boxPaint.setStyle(Paint.Style.STROKE);
        boxPaint.setStrokeWidth(6f);
        boxPaint.setColor(Color.GREEN);
        boxPaint.setAntiAlias(true);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(40f);
        textPaint.setAntiAlias(true);

        textBgPaint.setColor(Color.argb(180, 0, 0, 0));
        textBgPaint.setStyle(Paint.Style.FILL);
    }

    public void setDetections(List<Detection> detections) {
        if (detections == null) {
            this.detections = new ArrayList<>();
        } else {
            this.detections = detections;
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        for (Detection detection : detections) {
            RectF box = detection.getBox();

            boxPaint.setColor(getColorForLabel(detection.getLabel()));
            canvas.drawRect(box, boxPaint);

            String label = detection.getLabel() + " " +
                    String.format(Locale.US, "%.0f%%", detection.getConfidence() * 100f);

            float textWidth = textPaint.measureText(label);
            float textHeight = textPaint.getTextSize();

            float left = box.left;
            float top = Math.max(textHeight + 8f, box.top);

            canvas.drawRect(
                    left,
                    top - textHeight - 16f,
                    left + textWidth + 20f,
                    top,
                    textBgPaint
            );

            canvas.drawText(label, left + 10f, top - 10f, textPaint);
        }
    }

    private int getColorForLabel(String label) {
        if (label == null) return Color.GREEN;

        label = label.toLowerCase(Locale.US);

        if (label.contains("person")) return Color.RED;
        if (label.contains("chair")) return Color.YELLOW;
        if (label.contains("table")) return Color.CYAN;
        if (label.contains("stairs")) return Color.MAGENTA;
        if (label.contains("door")) return Color.BLUE;

        return Color.GREEN;
    }
}