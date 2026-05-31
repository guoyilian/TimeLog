package com.example.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {
    private Paint slicePaint;
    private Paint textPaint;
    private Paint percentPaint;
    private Paint centerPaint;
    private RectF rectF;

    private List<Slice> slices = new ArrayList<>();
    private int[] colors = {
            0xFF4A7A56, 0xFF6A9974, 0xFF8FB899, 0xFFA3C6A8,
            0xFF558B6E, 0xFF7AAB8E, 0xFF9CC0AA, 0xFFBCD5C4
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
        init();
    }

    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        slicePaint = new Paint();
        slicePaint.setStyle(Paint.Style.FILL);
        slicePaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(0xFF333333);
        textPaint.setTextSize(spToPx(12));
        textPaint.setAntiAlias(true);

        percentPaint = new Paint();
        percentPaint.setColor(0xFF666666);
        percentPaint.setTextSize(spToPx(10));
        percentPaint.setAntiAlias(true);

        centerPaint = new Paint();
        centerPaint.setColor(0xFFFFFFFF);
        centerPaint.setStyle(Paint.Style.FILL);
        centerPaint.setAntiAlias(true);

        rectF = new RectF();
    }

    private float spToPx(float sp) {
        return sp * getContext().getResources().getDisplayMetrics().scaledDensity;
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
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, heightSize);
        } else {
            int defaultHeight = (int) dpToPx(280);
            setMeasuredDimension(width, defaultHeight);
        }
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (slices.isEmpty()) {
            drawEmptyState(canvas);
            return;
        }

        int size = Math.min(getWidth(), getHeight()) - (int) dpToPx(20);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = size / 2;

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        float startAngle = -90;
        int colorIndex = 0;

        float innerRadius = radius * 0.48f;

        for (Slice slice : slices) {
            slicePaint.setColor(colors[colorIndex % colors.length]);
            float sweepAngle = slice.percentage * 3.6f;
            
            // 绘制扇区
            canvas.drawArc(rectF, startAngle, sweepAngle, true, slicePaint);
            
            // 计算扇区中心角度
            float midAngle = startAngle + sweepAngle / 2;
            double radian = Math.toRadians(midAngle);
            
            // 计算标签位置（在扇区内部，半径的 65% 处）
            float labelRadius = innerRadius + (radius - innerRadius) * 0.65f;
            float labelX = (float) (centerX + labelRadius * Math.cos(radian));
            float labelY = (float) (centerY + labelRadius * Math.sin(radian));
            
            // 绘制标签（名称 + 百分比）
            String label = String.format("%.0f%%", slice.percentage);
            textPaint.setColor(0xFFFFFFFF);
            textPaint.setTextSize(spToPx(12));  // 增大字体
            textPaint.setTextAlign(Paint.Align.CENTER);
            
            canvas.drawText(label, labelX, labelY, textPaint);
            
            // 如果名称较短，也显示名称
            if (slice.name.length() <= 4) {
                textPaint.setTextSize(spToPx(10));  // 增大字体
                canvas.drawText(slice.name, labelX, labelY + spToPx(14), textPaint);
            }
            
            startAngle += sweepAngle;
            colorIndex++;
        }

        centerPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(centerX, centerY, innerRadius, centerPaint);

        // 在中心圆内绘制提示文字
        textPaint.setColor(0xFF999999);
        textPaint.setTextSize(spToPx(10));
        textPaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("各计时", centerX, centerY - spToPx(4), textPaint);
        canvas.drawText("占比", centerX, centerY + spToPx(10), textPaint);
    }

    private void drawEmptyState(Canvas canvas) {
        int centerX = getWidth() / 2;
        int centerY = (int) (getHeight() - dpToPx(40)) / 2;
        int radius = Math.min(getWidth(), (int) (getHeight() - dpToPx(60))) / 2 - (int) dpToPx(8);

        slicePaint.setColor(0xFFEEEEEE);
        canvas.drawCircle(centerX, centerY, radius, slicePaint);

        centerPaint.setColor(0xFFFFFFFF);
        canvas.drawCircle(centerX, centerY, radius * 0.55f, centerPaint);
    }
}