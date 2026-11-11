package com.example.parktag;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.OvershootInterpolator;

/**
 * Automatic-transmission style gear shifter: a console plate with a gate slot,
 * P/R/N/D markings and a knob that can be dragged along the slot or tapped into place.
 * The knob snaps to the nearest gear with a haptic "click" at each detent.
 */
public class GearShifterView extends View {

    public interface OnGearChangeListener {
        void onGearChanged(int gear);
    }

    private static final String[] GEARS = {"P", "R", "N", "D"};

    private final Paint platePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint plateEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint slotEdgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint detentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint letterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint descPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobShadowPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobRimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobCapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint knobLetterPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final RectF plateRect = new RectF();
    private final RectF slotRect = new RectF();

    private final float density;
    private final float knobRadius;
    private final float slotWidth;

    private final int colorActive;
    private final int colorInactive;
    private final int colorDescActive;

    private final String[] descriptions = new String[GEARS.length];

    private int gear = GEARS.length - 1;
    private float knobY;
    private float slotX;
    private float firstGearY;
    private float gearSpacing;

    private boolean dragging;
    private float dragOffset;
    private int lastDetent;
    private ValueAnimator snapAnimator;
    private OnGearChangeListener listener;

    public GearShifterView(Context context) {
        this(context, null);
    }

    public GearShifterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        knobRadius = 26f * density;
        slotWidth = 14f * density;

        colorActive = context.getColor(R.color.shifter_active);
        colorInactive = context.getColor(R.color.shifter_inactive);
        colorDescActive = context.getColor(R.color.shifter_text);

        plateEdgePaint.setStyle(Paint.Style.STROKE);
        plateEdgePaint.setStrokeWidth(1f * density);
        plateEdgePaint.setColor(context.getColor(R.color.shifter_console_edge));

        slotPaint.setColor(context.getColor(R.color.shifter_slot));
        slotEdgePaint.setStyle(Paint.Style.STROKE);
        slotEdgePaint.setStrokeWidth(1f * density);
        slotEdgePaint.setColor(context.getColor(R.color.shifter_console_edge));

        detentPaint.setStrokeWidth(2f * density);
        detentPaint.setStrokeCap(Paint.Cap.ROUND);
        detentPaint.setColor(context.getColor(R.color.shifter_console_edge));

        letterPaint.setTextAlign(Paint.Align.CENTER);
        letterPaint.setTextSize(sp(24));
        letterPaint.setFakeBoldText(true);

        descPaint.setTextSize(sp(15));

        knobRimPaint.setStyle(Paint.Style.STROKE);
        knobRimPaint.setStrokeWidth(1.5f * density);
        knobRimPaint.setColor(0xFF0E1014);

        knobLetterPaint.setTextAlign(Paint.Align.CENTER);
        knobLetterPaint.setTextSize(sp(16));
        knobLetterPaint.setFakeBoldText(true);
        knobLetterPaint.setColor(colorActive);

        // Knob gradients are built around (0,0); onDraw translates the canvas to the knob.
        knobShadowPaint.setShader(new RadialGradient(0f, 5f * density, knobRadius + 10f * density,
                new int[]{0x99000000, 0x00000000}, new float[]{0.6f, 1f}, Shader.TileMode.CLAMP));
        knobPaint.setShader(new RadialGradient(-knobRadius * 0.35f, -knobRadius * 0.45f,
                knobRadius * 1.6f,
                new int[]{0xFF7A8292, 0xFF363C48, 0xFF181B21},
                new float[]{0f, 0.5f, 1f}, Shader.TileMode.CLAMP));
        knobCapPaint.setShader(new LinearGradient(0f, -knobRadius, 0f, knobRadius,
                0xFF1C2028, 0xFF2C313B, Shader.TileMode.CLAMP));

        setClickable(true);
        setFocusable(true);
        updateContentDescription();
    }

    public void setOnGearChangeListener(OnGearChangeListener listener) {
        this.listener = listener;
    }

    public void setDescriptions(String... descs) {
        System.arraycopy(descs, 0, descriptions, 0, Math.min(descs.length, descriptions.length));
        invalidate();
    }

    public int getGear() {
        return gear;
    }

    /** Moves the knob without notifying the listener. */
    public void setGear(int newGear, boolean animate) {
        gear = newGear;
        updateContentDescription();
        if (gearSpacing == 0f) {
            return; // Not laid out yet; onSizeChanged places the knob.
        }
        if (animate) {
            animateKnobTo(gearY(newGear));
        } else {
            cancelSnap();
            knobY = gearY(newGear);
            invalidate();
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        float inset = 1f * density;
        plateRect.set(inset, inset, w - inset, h - inset);
        platePaint.setShader(new LinearGradient(0f, 0f, 0f, h,
                getContext().getColor(R.color.shifter_console_top),
                getContext().getColor(R.color.shifter_console_bottom),
                Shader.TileMode.CLAMP));

        float verticalPad = knobRadius + 18f * density;
        firstGearY = verticalPad;
        gearSpacing = (h - 2 * verticalPad) / (GEARS.length - 1);
        slotX = 96f * density;

        float slotOverhang = slotWidth;
        slotRect.set(slotX - slotWidth / 2f, firstGearY - slotOverhang,
                slotX + slotWidth / 2f, gearY(GEARS.length - 1) + slotOverhang);

        cancelSnap();
        knobY = gearY(gear);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float corner = 24f * density;
        canvas.drawRoundRect(plateRect, corner, corner, platePaint);
        canvas.drawRoundRect(plateRect, corner, corner, plateEdgePaint);

        canvas.drawRoundRect(slotRect, slotWidth / 2f, slotWidth / 2f, slotPaint);
        canvas.drawRoundRect(slotRect, slotWidth / 2f, slotWidth / 2f, slotEdgePaint);

        int highlighted = nearestGear(knobY);
        float letterX = slotX - knobRadius - 26f * density;
        float descX = slotX + knobRadius + 22f * density;
        float detentGap = 4f * density;
        float detentLen = 7f * density;

        for (int i = 0; i < GEARS.length; i++) {
            float y = gearY(i);
            boolean active = i == highlighted;

            // Detent notches on both sides of the slot
            float left = slotRect.left - detentGap;
            float right = slotRect.right + detentGap;
            canvas.drawLine(left - detentLen, y, left, y, detentPaint);
            canvas.drawLine(right, y, right + detentLen, y, detentPaint);

            letterPaint.setColor(active ? colorActive : colorInactive);
            canvas.drawText(GEARS[i], letterX, baseline(letterPaint, y), letterPaint);

            if (descriptions[i] != null) {
                descPaint.setColor(active ? colorDescActive : colorInactive);
                descPaint.setFakeBoldText(active);
                canvas.drawText(descriptions[i], descX, baseline(descPaint, y), descPaint);
            }
        }

        drawKnob(canvas, highlighted);
    }

    private void drawKnob(Canvas canvas, int highlighted) {
        int save = canvas.save();
        canvas.translate(slotX, knobY);

        canvas.drawCircle(0f, 0f, knobRadius + 10f * density, knobShadowPaint);
        canvas.drawCircle(0f, 0f, knobRadius, knobPaint);
        canvas.drawCircle(0f, 0f, knobRadius, knobRimPaint);

        float capRadius = knobRadius * 0.58f;
        canvas.drawCircle(0f, 0f, capRadius, knobCapPaint);
        canvas.drawText(GEARS[highlighted], 0f, baseline(knobLetterPaint, 0f), knobLetterPaint);

        canvas.restoreToCount(save);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                cancelSnap();
                dragging = Math.abs(y - knobY) < knobRadius * 1.5f
                        && Math.abs(x - slotX) < knobRadius * 2f;
                if (dragging) {
                    dragOffset = knobY - y;
                    lastDetent = nearestGear(knobY);
                    // Keep the surrounding scroll view from stealing the drag
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    knobY = clamp(y + dragOffset, gearY(0), gearY(GEARS.length - 1));
                    int detent = nearestGear(knobY);
                    if (detent != lastDetent) {
                        lastDetent = detent;
                        performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
                    }
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dragging = false;
                    selectGear(nearestGear(knobY));
                } else {
                    performClick();
                    selectGear(nearestGear(y));
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                dragging = false;
                animateKnobTo(gearY(gear));
                return true;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    private void selectGear(int target) {
        animateKnobTo(gearY(target));
        if (target != gear) {
            gear = target;
            updateContentDescription();
            performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);
            if (listener != null) {
                listener.onGearChanged(target);
            }
        }
    }

    private void animateKnobTo(float targetY) {
        cancelSnap();
        snapAnimator = ValueAnimator.ofFloat(knobY, targetY);
        snapAnimator.setDuration(260);
        snapAnimator.setInterpolator(new OvershootInterpolator(1.4f));
        snapAnimator.addUpdateListener(a -> {
            knobY = (float) a.getAnimatedValue();
            invalidate();
        });
        snapAnimator.start();
    }

    private void cancelSnap() {
        if (snapAnimator != null) {
            snapAnimator.cancel();
            snapAnimator = null;
        }
    }

    private void updateContentDescription() {
        setContentDescription("Gear shifter, " + GEARS[gear]);
    }

    private float gearY(int index) {
        return firstGearY + index * gearSpacing;
    }

    private int nearestGear(float y) {
        if (gearSpacing == 0f) {
            return gear;
        }
        int index = Math.round((y - firstGearY) / gearSpacing);
        return Math.max(0, Math.min(GEARS.length - 1, index));
    }

    private static float baseline(Paint paint, float centerY) {
        Paint.FontMetrics fm = paint.getFontMetrics();
        return centerY - (fm.ascent + fm.descent) / 2f;
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private float sp(float value) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, value,
                getResources().getDisplayMetrics());
    }
}
