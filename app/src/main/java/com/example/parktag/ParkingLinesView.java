package com.example.parktag;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

/**
 * Draws rear-camera style parking guide lines over the camera preview.
 * Three zones: close (red), caution (yellow), safe (green).
 * Two converging boundary lines represent the car's width.
 */
public class ParkingLinesView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    public ParkingLinesView(Context context) {
        super(context);
        init();
    }

    public ParkingLinesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(3f);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(36f);
        textPaint.setFakeBoldText(true);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();

        // Vanishing point: center top-third
        float vpX = w / 2f;
        float vpY = h * 0.35f;

        // Car width at the bottom (as fraction of screen width)
        float carHalfWidth = w * 0.22f;
        float leftBottom  = vpX - carHalfWidth;
        float rightBottom = vpX + carHalfWidth;

        // Draw left and right boundary lines
        linePaint.setColor(0xCCFFFFFF);
        linePaint.setPathEffect(null);
        linePaint.setStrokeWidth(4f);
        canvas.drawLine(vpX, vpY, leftBottom,  h, linePaint);
        canvas.drawLine(vpX, vpY, rightBottom, h, linePaint);

        // Zone proportions (fraction of h from bottom)
        float[] zoneY = { h * 0.15f, h * 0.38f, h * 0.62f };
        int[]   colors = { 0xCCFF4444, 0xCCFFCC00, 0xCC44CC44 };
        String[] labels = { "CLOSE", "CAUTION", "SAFE" };

        for (int i = 0; i < zoneY.length; i++) {
            float y = zoneY[i];

            // Interpolate x bounds at this y using the boundary lines
            float t = (h - y) / (h - vpY);
            float lx = vpX + (leftBottom  - vpX) * t;
            float rx = vpX + (rightBottom - vpX) * t;

            linePaint.setColor(colors[i]);
            linePaint.setStrokeWidth(i == 0 ? 4f : 3f);
            linePaint.setPathEffect(i == 0 ? null : new DashPathEffect(new float[]{20f, 12f}, 0f));

            canvas.drawLine(lx, y, rx, y, linePaint);

            // Label
            textPaint.setColor(colors[i]);
            textPaint.setTextSize(28f);
            float textW = textPaint.measureText(labels[i]);
            canvas.drawText(labels[i], rx + 16f, y + 10f, textPaint);
        }

        // Arc near bottom suggesting bumper proximity
        linePaint.setColor(0xCCFF4444);
        linePaint.setStrokeWidth(3f);
        linePaint.setPathEffect(new DashPathEffect(new float[]{12f, 8f}, 0f));
        Path arc = new Path();
        arc.moveTo(leftBottom, h - 30f);
        arc.quadTo(vpX, h - 80f, rightBottom, h - 30f);
        canvas.drawPath(arc, linePaint);
    }
}
