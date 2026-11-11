package com.example.parktag;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

/**
 * Draws rear-camera style parking guide lines over the camera preview.
 * Three zones: close (red), caution (yellow), safe (green).
 * Two converging boundary lines represent the car's width.
 */
public class ParkingLinesView extends View {

    private final Paint linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float density;

    public ParkingLinesView(Context context) {
        super(context);
        init();
    }

    public ParkingLinesView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        density = getResources().getDisplayMetrics().density;

        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setStyle(Paint.Style.FILL);
        textPaint.setTextSize(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 12f, getResources().getDisplayMetrics()));
        textPaint.setFakeBoldText(true);
        textPaint.setLetterSpacing(0.08f);
        textPaint.setShadowLayer(4f * density, 0f, 0f, 0x99000000);
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
        linePaint.setStrokeWidth(2.5f * density);
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
            linePaint.setStrokeWidth((i == 0 ? 2.5f : 2f) * density);
            linePaint.setPathEffect(i == 0 ? null : new DashPathEffect(new float[]{8f * density, 5f * density}, 0f));

            canvas.drawLine(lx, y, rx, y, linePaint);

            // Label
            textPaint.setColor(colors[i]);
            canvas.drawText(labels[i], rx + 8f * density, y + textPaint.getTextSize() / 3f, textPaint);
        }

        // Arc near bottom suggesting bumper proximity
        linePaint.setColor(0xCCFF4444);
        linePaint.setStrokeWidth(2f * density);
        linePaint.setPathEffect(new DashPathEffect(new float[]{5f * density, 3f * density}, 0f));
        Path arc = new Path();
        arc.moveTo(leftBottom, h - 12f * density);
        arc.quadTo(vpX, h - 32f * density, rightBottom, h - 12f * density);
        canvas.drawPath(arc, linePaint);
    }
}
