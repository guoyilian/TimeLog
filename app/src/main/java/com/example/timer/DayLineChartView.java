package com.example.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class DayLineChartView extends BaseChartView {

    private static final float DEFAULT_X_AXIS_START = 0f;
    private static final float DEFAULT_X_AXIS_END = 24f;

    private float xAxisStart;
    private float xAxisEnd;
    private float xAxisRange;

    private List<DataPoint> dataPoints;
    private Paint linePaint;
    private Paint fillPaint;
    private Paint gridPaint;
    private Paint yTextPaint;
    private Paint xTextPaint;
    private Paint tooltipPaint;
    private Paint tooltipBorderPaint;
    private Paint tooltipTextPaint;

    private Path linePath;
    private Path fillPath;

    private float[] xAxisLabelHours;
    private String[] xAxisLabels;

    private float chartWidth;
    private float chartHeight;
    private float plotLeft;
    private float plotRight;
    private float plotWidth;
    private float paddingLeft;
    private float paddingRight;
    private float paddingTop;
    private float paddingBottom;

    private float maxYValue;
    private float[] yAxisLabels;
    private int totalMinutes;
    private String titlePrefix;

    private boolean isTouching = false;
    private boolean isTooltipVisible = false;
    private float touchX = 0;
    private float touchY = 0;
    private DataPoint touchedPoint = null;
    private float touchedDataX = 0;
    private float touchedDataY = 0;

    public static class DataPoint {
        public float hour;
        public float minutes;
        public String label;
        public String timeLabel;

        public DataPoint(float hour, float minutes, String label, String timeLabel) {
            this.hour = hour;
            this.minutes = minutes;
            this.label = label;
            this.timeLabel = timeLabel;
        }
    }

    public DayLineChartView(Context context) {
        super(context);
    }

    public DayLineChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public DayLineChartView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void init(AttributeSet attrs) {
        xAxisStart = DEFAULT_X_AXIS_START;
        xAxisEnd = DEFAULT_X_AXIS_END;
        xAxisRange = xAxisEnd - xAxisStart;
        dataPoints = new ArrayList<>();
        xAxisLabelHours = new float[]{0f, 6f, 12f, 18f, 24f};
        xAxisLabels = new String[]{"00:00", "06:00", "12:00", "18:00", "24:00"};
        maxYValue = 60f;
        totalMinutes = 0;
        titlePrefix = "今日";

        linePaint = createPaint();
        linePaint.setColor(Color.parseColor("#6B9080"));
        linePaint.setStrokeWidth(dpToPx(2.5f));
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeCap(Paint.Cap.ROUND);
        linePaint.setStrokeJoin(Paint.Join.ROUND);

        fillPaint = createPaint();
        fillPaint.setColor(Color.parseColor("#99E6F4EA"));
        fillPaint.setStyle(Paint.Style.FILL);

        gridPaint = createPaint();
        gridPaint.setColor(Color.parseColor("#EEEEEE"));
        gridPaint.setStrokeWidth(dpToPx(1f));
        gridPaint.setStyle(Paint.Style.STROKE);
        gridPaint.setPathEffect(new DashPathEffect(new float[]{dpToPx(4), dpToPx(4)}, 0));

        yTextPaint = createPaint();
        yTextPaint.setColor(Color.parseColor("#666666"));
        yTextPaint.setTextSize(spToPx(11));
        yTextPaint.setTextAlign(Paint.Align.RIGHT);

        xTextPaint = createPaint();
        xTextPaint.setColor(Color.parseColor("#666666"));
        xTextPaint.setTextSize(spToPx(11));
        xTextPaint.setTextAlign(Paint.Align.CENTER);

        tooltipPaint = createPaint();
        tooltipPaint.setColor(Color.TRANSPARENT);
        tooltipPaint.setStyle(Paint.Style.FILL);

        tooltipBorderPaint = createPaint();
        tooltipBorderPaint.setColor(Color.parseColor("#CCCCCC"));
        tooltipBorderPaint.setStyle(Paint.Style.STROKE);
        tooltipBorderPaint.setStrokeWidth(dpToPx(1));

        tooltipTextPaint = createPaint();
        tooltipTextPaint.setColor(Color.parseColor("#CCCCCC"));
        tooltipTextPaint.setTextSize(spToPx(11));
        tooltipTextPaint.setTextAlign(Paint.Align.CENTER);

        linePath = new Path();
        fillPath = new Path();

        paddingRight = dpToPx(8);
        paddingTop = dpToPx(70);
        paddingBottom = dpToPx(28);

        initEmptyData();
        calculateYAxisLabels(15f);
        updatePaddings();
    }

    public void setTitlePrefix(String prefix) {
        this.titlePrefix = prefix;
        invalidate();
    }

    private void updatePaddings() {
        float maxLabelWidth = 0;
        if (yAxisLabels != null) {
            for (float val : yAxisLabels) {
                maxLabelWidth = Math.max(maxLabelWidth, yTextPaint.measureText(getYAxisLabelText(val)));
            }
        }
        paddingLeft = (int) maxLabelWidth + dpToPx(12);
        paddingRight = dpToPx(8);
    }
    
    private String getYAxisLabelText(float minutes) {
        float hours = minutes / 60f;
        if (hours < 1) {
            return String.format("%.0f分钟", minutes);
        } else if (hours == 1) {
            return "1小时";
        } else if (hours % 1 == 0) {
            return String.format("%.0f小时", hours);
        } else {
            return String.format("%.1f小时", hours);
        }
    }

    private void initEmptyData() {
        dataPoints.clear();
        for (int hour = (int) xAxisStart; hour <= (int) xAxisEnd; hour++) {
            dataPoints.add(new DataPoint(hour, 0, "", String.format("%02d:00", hour)));
        }
    }

    private void calculateYAxisLabels(float maxTime) {
        float maxHours = maxTime / 60f;
        
        if (maxHours <= 0.25f) { // ≤ 15分钟
            maxYValue = 15f;
            yAxisLabels = new float[]{0, 5, 10, 15};
        } else if (maxHours <= 0.5f) { // ≤ 30分钟
            maxYValue = 30f;
            yAxisLabels = new float[]{0, 10, 20, 30};
        } else if (maxHours <= 1f) { // ≤ 1小时
            maxYValue = 60f;
            yAxisLabels = new float[]{0, 15, 30, 45, 60};
        } else if (maxHours <= 2f) { // ≤ 2小时
            maxYValue = 120f;
            yAxisLabels = new float[]{0, 30, 60, 90, 120};
        } else if (maxHours <= 4f) { // ≤ 4小时
            maxYValue = 240f;
            yAxisLabels = new float[]{0, 60, 120, 180, 240};
        } else {
            int stepMinutes = 60; // 1小时为一个刻度
            int count = (int) Math.ceil(maxTime / stepMinutes);
            maxYValue = count * stepMinutes;
            yAxisLabels = new float[count + 1];
            for (int i = 0; i <= count; i++) {
                yAxisLabels[i] = i * stepMinutes;
            }
        }
        updatePaddings();
    }

    public void setData(List<TimerRecord> records) {
        dataPoints.clear();
        totalMinutes = 0;

        float[] hourMinutes = new float[25];
        String[] hourLabels = new String[25];
        float maxTime = 0;

        if (records != null) {
            for (TimerRecord record : records) {
                totalMinutes += record.getDurationMin();

                long start = record.getStart();
                int startHour = DateUtils.getHour(start);
                int duration = record.getDurationMin();
                String recordName = record.getName();

                int endHour = Math.min(24, startHour + (duration / 60) + 1);
                for (int h = startHour; h < endHour; h++) {
                    if (h < xAxisStart || h > xAxisEnd) {
                        continue;
                    }
                    hourMinutes[h] += duration;
                    if (hourMinutes[h] > maxTime) {
                        maxTime = hourMinutes[h];
                    }
                    if (hourLabels[h] == null || hourLabels[h].isEmpty()) {
                        hourLabels[h] = recordName;
                    }
                }
            }
        }

        if (maxTime == 0) {
            maxTime = 15f;
        }

        calculateYAxisLabels(maxTime);

        for (int hour = (int) xAxisStart; hour <= (int) xAxisEnd; hour++) {
            float minutes = hourMinutes[hour];
            String timeLabel = String.format("%02d:00", hour);
            String label = hourLabels[hour];
            if (label == null) {
                label = "";
            }
            dataPoints.add(new DataPoint(hour, minutes, label, timeLabel));
        }

        invalidate();
    }

    public void resetToToday() {
        titlePrefix = "今日";
        invalidate();
    }

    private float hourToX(float hour) {
        float normalized = (hour - xAxisStart) / xAxisRange;
        return plotLeft + normalized * plotWidth;
    }

    private void updatePlotBounds() {
        plotLeft = paddingLeft;
        plotRight = getWidth() - paddingRight;
        plotWidth = plotRight - plotLeft;
        chartWidth = plotWidth;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureDefaultHeight(widthMeasureSpec, heightMeasureSpec, 240);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updatePlotBounds();
        chartHeight = getHeight() - paddingTop - paddingBottom;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        updatePlotBounds();
        chartHeight = getHeight() - paddingTop - paddingBottom;

        drawTitle(canvas);
        drawGrid(canvas);
        drawYAxisLabels(canvas);
        drawXAxisLabels(canvas);
        drawLine(canvas);
        if (isTooltipVisible) {
            drawTooltip(canvas);
        }
    }

    private void drawTitle(Canvas canvas) {
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;

        Paint numPaint = new Paint();
        numPaint.setColor(Color.parseColor("#5A7D5A"));
        numPaint.setTextSize(spToPx(36));
        numPaint.setFakeBoldText(true);
        numPaint.setAntiAlias(true);
        numPaint.setTextAlign(Paint.Align.LEFT);

        Paint unitPaint = new Paint();
        unitPaint.setColor(Color.parseColor("#5A7D5A"));
        unitPaint.setTextSize(spToPx(18));
        unitPaint.setFakeBoldText(false);
        unitPaint.setAntiAlias(true);
        unitPaint.setTextAlign(Paint.Align.LEFT);

        String hourStr = String.valueOf(hours);
        String hourUnit = "小时";
        String minStr = String.valueOf(mins);
        String minUnit = "分钟";

        float hourWidth = numPaint.measureText(hourStr);
        float hourUnitWidth = unitPaint.measureText(hourUnit);
        float minWidth = numPaint.measureText(minStr);
        float minUnitWidth = unitPaint.measureText(minUnit);
        float gap = dpToPx(6);

        float totalWidth;
        if (hours > 0) {
            totalWidth = hourWidth + gap + hourUnitWidth + gap + minWidth + gap + minUnitWidth;
        } else {
            totalWidth = minWidth + gap + minUnitWidth;
        }

        float baselineY = dpToPx(48);
        float startX = (getWidth() - totalWidth) / 2;

        if (hours > 0) {
            canvas.drawText(hourStr, startX, baselineY, numPaint);
            canvas.drawText(hourUnit, startX + hourWidth + gap, baselineY, unitPaint);
            float afterHour = startX + hourWidth + gap + hourUnitWidth + gap;
            canvas.drawText(minStr, afterHour, baselineY, numPaint);
            canvas.drawText(minUnit, afterHour + minWidth + gap, baselineY, unitPaint);
        } else {
            canvas.drawText(minStr, startX, baselineY, numPaint);
            canvas.drawText(minUnit, startX + minWidth + gap, baselineY, unitPaint);
        }
    }

    private void drawGrid(Canvas canvas) {
        for (float yVal : yAxisLabels) {
            float y = paddingTop + chartHeight - (yVal / maxYValue) * chartHeight;
            canvas.drawLine(plotLeft, y, plotRight, y, gridPaint);
        }
    }

    private void drawXAxisLabels(Canvas canvas) {
        float y = paddingTop + chartHeight + dpToPx(16);

        for (int i = 0; i < xAxisLabels.length; i++) {
            float hour = xAxisLabelHours[i];
            if (i == 0) {
                xTextPaint.setTextAlign(Paint.Align.LEFT);
                canvas.drawText(xAxisLabels[i], plotLeft, y, xTextPaint);
            } else if (i == xAxisLabels.length - 1) {
                xTextPaint.setTextAlign(Paint.Align.RIGHT);
                canvas.drawText(xAxisLabels[i], plotRight, y, xTextPaint);
            } else {
                xTextPaint.setTextAlign(Paint.Align.CENTER);
                canvas.drawText(xAxisLabels[i], hourToX(hour), y, xTextPaint);
            }
        }
        xTextPaint.setTextAlign(Paint.Align.CENTER);
    }

    private void drawYAxisLabels(Canvas canvas) {
        for (float val : yAxisLabels) {
            float x = paddingLeft - dpToPx(6);
            float y = paddingTop + chartHeight - (val / maxYValue) * chartHeight + dpToPx(4);
            canvas.drawText(getYAxisLabelText(val), x, y, yTextPaint);
        }
    }

    private void drawLine(Canvas canvas) {
        if (dataPoints.size() < 2) return;

        linePath.reset();
        fillPath.reset();

        float startX = hourToX(dataPoints.get(0).hour);
        float startY = paddingTop + chartHeight - (dataPoints.get(0).minutes / maxYValue) * chartHeight;

        linePath.moveTo(startX, startY);
        fillPath.moveTo(startX, paddingTop + chartHeight);
        fillPath.lineTo(startX, startY);

        for (int i = 1; i < dataPoints.size(); i++) {
            float x = hourToX(dataPoints.get(i).hour);
            float y = paddingTop + chartHeight - (Math.min(dataPoints.get(i).minutes, maxYValue) / maxYValue) * chartHeight;

            float prevX = hourToX(dataPoints.get(i - 1).hour);
            float prevY = paddingTop + chartHeight - (Math.min(dataPoints.get(i - 1).minutes, maxYValue) / maxYValue) * chartHeight;

            float controlX1 = prevX + (x - prevX) * 0.2f;
            float controlY1 = prevY;
            float controlX2 = prevX + (x - prevX) * 0.8f;
            float controlY2 = y;

            linePath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y);
            fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x, y);
        }

        float endX = hourToX(dataPoints.get(dataPoints.size() - 1).hour);
        float endY = paddingTop + chartHeight;
        fillPath.lineTo(endX, endY);
        fillPath.close();

        canvas.drawPath(fillPath, fillPaint);
        canvas.drawPath(linePath, linePaint);
    }

    private void drawTooltip(Canvas canvas) {
        if (touchedPoint == null) return;

        float x = touchedDataX;
        float y = touchedDataY;

        float tooltipWidth = dpToPx(80);
        float tooltipHeight = dpToPx(36);
        float cornerRadius = dpToPx(6);

        float tooltipX = x - tooltipWidth / 2;
        float tooltipY = y - tooltipHeight - dpToPx(8);

        if (tooltipX < dpToPx(8)) tooltipX = dpToPx(8);
        if (tooltipX + tooltipWidth > getWidth() - dpToPx(8)) tooltipX = getWidth() - dpToPx(8) - tooltipWidth;
        if (tooltipY < dpToPx(8)) tooltipY = y + dpToPx(8);

        Path tooltipPath = new Path();
        tooltipPath.addRoundRect(tooltipX, tooltipY, tooltipX + tooltipWidth, tooltipY + tooltipHeight, cornerRadius, cornerRadius, Path.Direction.CW);

        canvas.drawPath(tooltipPath, tooltipPaint);
        canvas.drawPath(tooltipPath, tooltipBorderPaint);

        String line1 = touchedPoint.timeLabel;
        if (touchedPoint.label != null && !touchedPoint.label.isEmpty()) {
            line1 = touchedPoint.timeLabel + " " + touchedPoint.label;
        }
        String line2;
        float minutes = Math.max(touchedPoint.minutes, 0);
        float hours = minutes / 60f;
        if (hours < 1) {
            line2 = String.format("%.0f分钟", minutes);
        } else if (hours == 1) {
            line2 = "1小时";
        } else if (hours % 1 == 0) {
            line2 = String.format("%.0f小时", hours);
        } else {
            line2 = String.format("%.1f小时", hours);
        }

        float textY = tooltipY + dpToPx(14);

        canvas.drawText(line1, x, textY, tooltipTextPaint);
        canvas.drawText(line2, x, textY + dpToPx(12), tooltipTextPaint);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isTouching = true;
                touchX = x;
                touchY = y;

                if (isTooltipVisible && isPointInTooltip(x, y)) {
                    isTooltipVisible = false;
                    touchedPoint = null;
                    invalidate();
                    return true;
                }

                findNearestPoint(x);
                if (touchedPoint != null) {
                    isTooltipVisible = true;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                isTouching = true;
                touchX = x;
                touchY = y;
                findNearestPoint(x);
                if (touchedPoint != null) {
                    isTooltipVisible = true;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_UP:
                isTouching = false;
                if (touchedPoint != null) {
                    isTooltipVisible = true;
                }
                invalidate();
                return true;

            case MotionEvent.ACTION_CANCEL:
                isTouching = false;
                isTooltipVisible = false;
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private boolean isPointInTooltip(float x, float y) {
        if (touchedPoint == null) return false;

        float tooltipWidth = dpToPx(80);
        float tooltipHeight = dpToPx(36);
        float tooltipX = touchedDataX - tooltipWidth / 2;
        float tooltipY = touchedDataY - tooltipHeight - dpToPx(8);

        if (tooltipX < dpToPx(8)) tooltipX = dpToPx(8);
        if (tooltipX + tooltipWidth > getWidth() - dpToPx(8)) tooltipX = getWidth() - dpToPx(8) - tooltipWidth;
        if (tooltipY < dpToPx(8)) tooltipY = touchedDataY + dpToPx(8);

        return x >= tooltipX && x <= tooltipX + tooltipWidth && y >= tooltipY && y <= tooltipY + tooltipHeight;
    }

    private void findNearestPoint(float x) {
        float nearestDist = Float.MAX_VALUE;
        DataPoint nearest = null;
        float nearestX = 0;
        float nearestY = 0;

        for (DataPoint point : dataPoints) {
            float pointX = hourToX(point.hour);
            float dist = Math.abs(pointX - x);
            if (dist < nearestDist) {
                nearestDist = dist;
                nearest = point;
                nearestX = pointX;
                nearestY = paddingTop + chartHeight - (point.minutes / maxYValue) * chartHeight;
            }
        }

        if (nearest != null) {
            touchedPoint = nearest;
            touchedDataX = nearestX;
            touchedDataY = nearestY;
        }
    }

    public void clearTouchState() {
        isTouching = false;
        isTooltipVisible = false;
        touchedPoint = null;
        touchX = 0;
        touchY = 0;
        touchedDataX = 0;
        touchedDataY = 0;
        invalidate();
    }
}

