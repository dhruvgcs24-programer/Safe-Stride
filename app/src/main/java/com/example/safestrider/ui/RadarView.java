package com.example.safestrider.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

public class RadarView extends View {

    private final Paint circlePaint = new Paint();
    private final Paint sweepPaint = new Paint();
    private final Paint centerPaint = new Paint();
    private float sweepAngle = 0f;

    public RadarView(Context context) {
        super(context);
        init();
    }

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public RadarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        circlePaint.setStyle(Paint.Style.STROKE);
        circlePaint.setStrokeWidth(4f);
        circlePaint.setColor(Color.argb(160, 6, 182, 212));
        circlePaint.setAntiAlias(true);

        sweepPaint.setStyle(Paint.Style.STROKE);
        sweepPaint.setStrokeWidth(6f);
        sweepPaint.setColor(Color.argb(220, 79, 70, 229));
        sweepPaint.setAntiAlias(true);

        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setColor(Color.argb(255, 6, 182, 212));
        centerPaint.setAntiAlias(true);

        startSweepAnimation();
    }

    private void startSweepAnimation() {
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 360f);
        animator.setDuration(2200);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(animation -> {
            sweepAngle = (float) animation.getAnimatedValue();
            invalidate();
        });
        animator.start();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        float cx = width / 2f;
        float cy = height / 2f;

        float radius = Math.min(width, height) / 2f - 12f;

        // Radar circles
        canvas.drawCircle(cx, cy, radius, circlePaint);
        canvas.drawCircle(cx, cy, radius * 0.66f, circlePaint);
        canvas.drawCircle(cx, cy, radius * 0.33f, circlePaint);

        // Cross lines
        canvas.drawLine(cx - radius, cy, cx + radius, cy, circlePaint);
        canvas.drawLine(cx, cy - radius, cx, cy + radius, circlePaint);

        // Sweep line
        double angleRad = Math.toRadians(sweepAngle - 90);
        float endX = cx + (float) (radius * Math.cos(angleRad));
        float endY = cy + (float) (radius * Math.sin(angleRad));
        canvas.drawLine(cx, cy, endX, endY, sweepPaint);

        // Center dot
        canvas.drawCircle(cx, cy, 10f, centerPaint);
    }
}