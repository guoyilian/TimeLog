package com.example.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class SolidPieChartView extends View {
    private Paint slicePaint;
    private Paint textPaint;
    private RectF rectF;

    private List<Slice> slices = new ArrayList<>();
    
    // 公开的颜色数组，供外部使用
    public static final int[] COLORS = {
            0xDDFFB6C1, 0xDD98D8C8, 0xDDF7DC6F, 0xDDBB8FCE,
            0xDD85C1E9, 0xDDF8B500, 0xDDF1948A, 0xDDD7BDE2
    };
    
    private int[] colors = COLORS;

    public static class Slice {
        public String name;
        public int minutes;
        public float percentage;

        public Slice(String name, int minutes) {
            this.name = name;
            this.minutes = minutes;
        }
    }

    public SolidPieChartView(Context context) {
        super(context);
        init();
    }

    public SolidPieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SolidPieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
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

        int size = Math.min(getWidth(), getHeight()) - (int) dpToPx(10);
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = size / 2;

        rectF.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);

        float startAngle = -90;
        int colorIndex = 0;

        for (Slice slice : slices) {
            slicePaint.setColor(colors[colorIndex % colors.length]);
            float sweepAngle = slice.percentage * 3.6f;
            
            // 绘制实心扇区
            canvas.drawArc(rectF, startAngle, sweepAngle, true, slicePaint);
            
            // 只有占比大于等于 3% 的扇区才显示标签，避免重叠
            if (slice.percentage >= 3) {
                // 计算扇区中心角度
                float midAngle = startAngle + sweepAngle / 2;
                double radian = Math.toRadians(midAngle);
                
                // 计算标签位置（在扇区内部，半径的 65% 处）
                float labelRadius = radius * 0.65f;
                float labelX = (float) (centerX + labelRadius * Math.cos(radian));
                float labelY = (float) (centerY + labelRadius * Math.sin(radian));
                
                // 绘制标签（百分比）
                String label = String.format("%.0f%%", slice.percentage);
                textPaint.setTextSize(spToPx(12));
                textPaint.setTextAlign(Paint.Align.CENTER);
                
                // 处理过长的名称，最多显示3个字符，超出部分用省略号
                String displayName = slice.name;
                if (slice.name.length() > 3) {
                    displayName = slice.name.substring(0, 3) + "…";
                }
                
                // 先绘制名称，再绘制百分比
                canvas.drawText(displayName, labelX, labelY - spToPx(6), textPaint);
                canvas.drawText(label, labelX, labelY + spToPx(10), textPaint);
            }
            
            startAngle += sweepAngle;
            colorIndex++;
        }
    }

    private void drawEmptyState(Canvas canvas) {
        int centerX = getWidth() / 2;
        int centerY = getHeight() / 2;
        int radius = Math.min(getWidth(), getHeight()) / 2 - (int) dpToPx(10);

        slicePaint.setColor(0xFFE0E0E0);
        canvas.drawCircle(centerX, centerY, radius, slicePaint);
    }
}
