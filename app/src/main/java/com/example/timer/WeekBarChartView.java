package com.example.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class WeekBarChartView extends View {

    private static final float[] Y_LABELS = {0, 3, 6, 9, 12};
    private static final float Y_MAX = 12f;

    private Paint barPaint;
    private Paint gridPaint;
    private Paint textPaint;

    private List<DayData> dayDataList = new ArrayList<>();
    private int totalMinutes = 0;

    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    private int chartWidth;
    private int chartHeight;

    public static class DayData {
        public String dayOfWeek;
        public String date;
        public int minutes;
        public String taskType;

        public DayData(String dayOfWeek, String date, int minutes, String taskType) {
            this.dayOfWeek = dayOfWeek;
            this.date = date;
            this.minutes = minutes;
            this.taskType = taskType;
        }
    }

    public WeekBarChartView(Context context) {
        super(context);
        init();
    }

    public WeekBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public WeekBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setColor(Color.parseColor("#6A9974"));
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setAntiAlias(true);

        gridPaint = new Paint();
        gridPaint.setColor(Color.parseColor("#EEEEEE"));
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setAntiAlias(true);

        textPaint = new Paint();
        textPaint.setColor(Color.parseColor("#666666"));
        textPaint.setTextSize(spToPx(11));
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);

        updatePaddings();
    }

    private void updatePaddings() {
        Paint labelPaint = new Paint();
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setAntiAlias(true);

        float maxLabelWidth = 0;
        for (float label : Y_LABELS) {
            maxLabelWidth = Math.max(maxLabelWidth, labelPaint.measureText(String.valueOf((int) label)));
        }

        paddingLeft = (int) maxLabelWidth + dpToPx(12);
        paddingRight = dpToPx(8);
        paddingTop = dpToPx(46);
        paddingBottom = dpToPx(28);
    }

    public void setData(List<DayData> data) {
        this.dayDataList = data != null ? data : new ArrayList<>();
        this.totalMinutes = 0;
        for (DayData day : dayDataList) {
            totalMinutes += day.minutes;
        }
        invalidate();
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, heightSize);
        } else {
            int defaultHeight = dpToPx(240);
            setMeasuredDimension(width, defaultHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        chartWidth = w - paddingLeft - paddingRight;
        chartHeight = h - paddingTop - paddingBottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawTitle(canvas);
        drawGrid(canvas);
        drawYAxisLabels(canvas);
        drawBars(canvas);
        drawXAxisLabels(canvas);
    }

    private void drawTitle(Canvas canvas) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        String text;
        if (hours > 0) {
            text = "本周总时长：" + hours + "小时" + mins + "分钟";
        } else {
            text = "本周总时长：" + mins + "分钟";
        }

        Paint titlePaint = new Paint();
        titlePaint.setColor(Color.parseColor("#5A7D5A"));
        titlePaint.setTextSize(spToPx(14));
        titlePaint.setFakeBoldText(true);
        titlePaint.setAntiAlias(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text, getWidth() / 2f, dpToPx(28), titlePaint);
    }

    private void drawGrid(Canvas canvas) {
        for (float yLabel : Y_LABELS) {
            float y = paddingTop + ((Y_MAX - yLabel) / Y_MAX) * chartHeight;
            canvas.drawLine(paddingLeft, y, getWidth() - paddingRight, y, gridPaint);
        }
    }

    private void drawYAxisLabels(Canvas canvas) {
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#666666"));
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setAntiAlias(true);
        labelPaint.setTextAlign(Paint.Align.RIGHT);

        for (float yLabel : Y_LABELS) {
            float y = paddingTop + ((Y_MAX - yLabel) / Y_MAX) * chartHeight;
            canvas.drawText(String.valueOf((int) yLabel) + "h", paddingLeft - dpToPx(6), y + dpToPx(4), labelPaint);
        }
    }

    private void drawBars(Canvas canvas) {
        if (dayDataList.isEmpty()) return;

        int barWidth = dpToPx(24);
        int barGap = dpToPx(8);
        int totalBarWidth = dayDataList.size() * barWidth + (dayDataList.size() - 1) * barGap;
        int startX = paddingLeft + (chartWidth - totalBarWidth) / 2;

        for (int i = 0; i < dayDataList.size(); i++) {
            DayData data = dayDataList.get(i);
            float barHeight = (data.minutes / 60f / Y_MAX) * chartHeight;
            float left = startX + i * (barWidth + barGap);
            float top = paddingTop + chartHeight - barHeight;
            float right = left + barWidth;
            float bottom = paddingTop + chartHeight;

            if (barHeight > 0) {
                drawRoundTopBar(canvas, left, top, right, bottom, barWidth / 2f);
            }
        }
    }

    private void drawRoundTopBar(Canvas canvas, float left, float top, float right, float bottom, float radius) {
        Path path = new Path();
        path.moveTo(left, bottom);
        path.lineTo(left, top + radius);
        path.quadTo(left, top, left + radius, top);
        path.lineTo(right - radius, top);
        path.quadTo(right, top, right, top + radius);
        path.lineTo(right, bottom);
        path.close();
        canvas.drawPath(path, barPaint);
    }

    private void drawXAxisLabels(Canvas canvas) {
        if (dayDataList.isEmpty()) return;

        int barWidth = dpToPx(24);
        int barGap = dpToPx(8);
        int totalBarWidth = dayDataList.size() * barWidth + (dayDataList.size() - 1) * barGap;
        int startX = paddingLeft + (chartWidth - totalBarWidth) / 2;

        Paint labelPaint = new Paint(textPaint);
        labelPaint.setTextAlign(Paint.Align.CENTER);

        for (int i = 0; i < dayDataList.size(); i++) {
            DayData data = dayDataList.get(i);
            float centerX = startX + i * (barWidth + barGap) + barWidth / 2f;
            float y = paddingTop + chartHeight + dpToPx(16);
            canvas.drawText(data.dayOfWeek, centerX, y, labelPaint);
        }
    }

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}

