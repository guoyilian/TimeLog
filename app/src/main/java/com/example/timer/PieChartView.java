package com.example.timer;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class PieChartView extends BaseChartView {
    private Paint slicePaint;
    private Paint textPaint;
    private Paint centerPaint;
    private RectF rectF;

    private List<Slice> slices;
    private int[] colors;

    private boolean isRingStyle;
    private int outerPadding;
    private int minPercentageToShowLabel;
    private int maxNameLength;

    public static final int[] DEFAULT_RING_COLORS = {
            0xFF4A7A56, 0xFF6A9974, 0xFF8FB899, 0xFFA3C6A8,
            0xFF558B6E, 0xFF7AAB8E, 0xFF9CC0AA, 0xFFBCD5C4
    };

    public static final int[] DEFAULT_SOLID_COLORS = {
            0xFF98D8C8, 0xFFFFB6C1, 0xFF85C1E9, 0xFFF7DC6F,
            0xFFBB8FCE, 0xFFF8B500, 0xFFF1948A, 0xFFD7BDE2
    };

    public static class Slice {
        public String name;
        public int minutes;
        public float percentage;

        public Slice(String name, int minutes) {
            this.name = name;
            this.minutes = minutes;
        }
    }

    public PieChartView(Context context) {
        super(context);
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(AttributeSet attrs) {
        slices = new ArrayList<>();

        if (attrs != null) {
            TypedArray a = getContext().obtainStyledAttributes(attrs, R.styleable.PieChartView);
            isRingStyle = a.getBoolean(R.styleable.PieChartView_ringStyle, true);
            a.recycle();
        } else {
            isRingStyle = true;
        }

        if (isRingStyle) {
            colors = DEFAULT_RING_COLORS;
            outerPadding = (int) dpToPx(20);
            minPercentageToShowLabel = 0;
            maxNameLength = 4;
        } else {
            colors = DEFAULT_SOLID_COLORS;
            outerPadding = (int) dpToPx(10);
            minPercentageToShowLabel = 3;
            maxNameLength = 3;
        }

        slicePaint = createPaint();
        slicePaint.setStyle(Paint.Style.FILL);

        textPaint = createPaint();
        textPaint.setColor(0xFF333333);
        textPaint.setTextSize(spToPx(12));

        centerPaint = createPaint(0xFFFFFFFF, Paint.Style.FILL);

        rectF = new RectF();
    }

    public void setRingStyle(boolean ringStyle) {
        this.isRingStyle = ringStyle;
        if (ringStyle) {
            this.colors = DEFAULT_RING_COLORS;
            this.outerPadding = (int) dpToPx(20);
            this.minPercentageToShowLabel = 0;
            this.maxNameLength = 4;
        } else {
            this.colors = DEFAULT_SOLID_COLORS;
            this.outerPadding = (int) dpToPx(10);
            this.minPercentageToShowLabel = 3;
            this.maxNameLength = 3;
        }
        invalidate();
    }

    public void setColors(int[] colors) {
        this.colors = colors != null ? colors : DEFAULT_RING_COLORS;
        invalidate();
    }

    public void setData(List<Slice> data) {
        this.slices = data != null ? data : new ArrayList<>();
        calculatePercentages();
        invalidate();
    }

    private void calculatePercentages() {
        int total = 0;
        for (Slice slice : slices) {
            total += slice.minutes;
        }
        for (Slice slice : slices) {
            slice.percentage = total > 0 ? (slice.minutes * 100f / total) : 0;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureDefaultHeight(widthMeasureSpec, heightMeasureSpec, 280);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slices.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        int size = Math.min(getWidth(), getHeight()) - outerPadding;
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = size / 2;

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        float startAngle = -90;
        int colorIndex = 0;

        float innerRadius = isRingStyle ? radius * 0.48f : 0;

        for (Slice slice : slices) {
            slicePaint.setColor(colors[colorIndex % colors.length]);
            float sweepAngle = slice.percentage * 3.6f;

            canvas.drawArc(rectF, startAngle, sweepAngle, true, slicePaint);

            if (slice.percentage >= minPercentageToShowLabel) {
                float midAngle = startAngle + sweepAngle / 2;
                double radian = Math.toRadians(midAngle);

                float labelRadius;
                if (isRingStyle) {
                    labelRadius = innerRadius + (radius - innerRadius) * 0.65f;
                } else {
                    labelRadius = radius * 0.65f;
                }
                float labelX = (float) (centerX + labelRadius * Math.cos(radian));
                float labelY = (float) (centerY + labelRadius * Math.sin(radian));

                String label = String.format("%.0f%%", slice.percentage);

                if (isRingStyle) {
                    textPaint.setColor(0xFFFFFFFF);
                    textPaint.setTextSize(spToPx(11));
                } else {
                    textPaint.setColor(0xFF333333);
                    textPaint.setTextSize(spToPx(12));
                }
                textPaint.setTextAlign(Paint.Align.CENTER);

                String displayName = slice.name;
                if (slice.name != null && slice.name.length() > maxNameLength) {
                    displayName = slice.name.substring(0, maxNameLength) + "…";
                }

                canvas.drawText(displayName, labelX, labelY - spToPx(6), textPaint);
                canvas.drawText(label, labelX, labelY + spToPx(10), textPaint);
            }

            startAngle += sweepAngle;
            colorIndex++;
        }

        if (isRingStyle) {
            centerPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(centerX, centerY, innerRadius, centerPaint);

            textPaint.setColor(0xFF999999);
            textPaint.setTextSize(spToPx(10));
            textPaint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText("各计时", centerX, centerY - spToPx(4), textPaint);
            canvas.drawText("占比", centerX, centerY + spToPx(10), textPaint);
        }
    }

    private void drawEmptyState(Canvas canvas) {
        int centerX = getWidth() / 2;
        int centerY;
        int radius;

        if (isRingStyle) {
            centerY = (int) (getHeight() - dpToPx(40)) / 2;
            radius = Math.min(getWidth(), (int) (getHeight() - dpToPx(60))) / 2 - (int) dpToPx(8);
        } else {
            centerY = getHeight() / 2;
            radius = Math.min(getWidth(), getHeight()) / 2 - (int) dpToPx(10);
        }

        if (isRingStyle) {
            slicePaint.setColor(0xFFEEEEEE);
        } else {
            slicePaint.setColor(0xFFE0E0E0);
        }
        canvas.drawCircle(centerX, centerY, radius, slicePaint);

        if (isRingStyle) {
            centerPaint.setColor(0xFFFFFFFF);
            canvas.drawCircle(centerX, centerY, radius * 0.55f, centerPaint);
        }
    }
}
