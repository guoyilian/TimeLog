package com.example.timer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class WeekBarChartView extends BaseChartView {

    private float[] yAxisLabels;
    private float maxYValue;

    private Paint barPaint;
    private Paint gridPaint;
    private Paint textPaint;

    private List<DayData> dayDataList;
    private List<RectF> barBoundsList;
    private int totalMinutes;

    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    private int chartWidth;
    private int chartHeight;

    private OnBarClickListener onBarClickListener;

    private int touchedBarIndex = -1;
    private Paint pressedBarPaint;
    private static final int PRESS_EXPAND_DP = 3;

    public interface OnBarClickListener {
        void onBarClick(DayData dayData, int index);
    }

    public static class DayData {
        public String dayOfWeek;
        public String date;
        public String fullDate;
        public int minutes;
        public String taskType;

        public DayData(String dayOfWeek, String date, String fullDate, int minutes, String taskType) {
            this.dayOfWeek = dayOfWeek;
            this.date = date;
            this.fullDate = fullDate;
            this.minutes = minutes;
            this.taskType = taskType;
        }
    }

    public WeekBarChartView(Context context) {
        super(context);
    }

    public WeekBarChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public WeekBarChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void setOnBarClickListener(OnBarClickListener listener) {
        this.onBarClickListener = listener;
    }

    public void clearTouchState() {
        touchedBarIndex = -1;
        invalidate();
    }

    @Override
    protected void init(AttributeSet attrs) {
        dayDataList = new ArrayList<>();
        barBoundsList = new ArrayList<>();
        totalMinutes = 0;

        barPaint = createPaint();
        barPaint.setColor(Color.parseColor("#6A9974"));
        barPaint.setStyle(Paint.Style.FILL);

        pressedBarPaint = createPaint();
        pressedBarPaint.setColor(Color.parseColor("#4A7D5A"));
        pressedBarPaint.setStyle(Paint.Style.FILL);

        gridPaint = createPaint();
        gridPaint.setColor(Color.parseColor("#EEEEEE"));
        gridPaint.setStrokeWidth(1);
        gridPaint.setStyle(Paint.Style.STROKE);

        textPaint = createPaint();
        textPaint.setColor(Color.parseColor("#666666"));
        textPaint.setTextSize(spToPx(11));
        textPaint.setTextAlign(Paint.Align.CENTER);

        calculateYAxisLabels(12f);
        updatePaddings();
    }

    private void updatePaddings() {
        Paint labelPaint = new Paint();
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setAntiAlias(true);

        float maxLabelWidth = 0;
        if (yAxisLabels != null) {
            for (float label : yAxisLabels) {
                maxLabelWidth = Math.max(maxLabelWidth, labelPaint.measureText(String.valueOf((int) label) + "h"));
            }
        }

        paddingLeft = (int) maxLabelWidth + dpToPx(12);
        paddingRight = dpToPx(8);
        paddingTop = dpToPx(100);
        paddingBottom = dpToPx(28);
    }

    private void calculateYAxisLabels(float maxHours) {
        if (maxHours <= 3) {
            maxYValue = 3f;
            yAxisLabels = new float[]{0, 1, 2, 3};
        } else if (maxHours <= 6) {
            maxYValue = 6f;
            yAxisLabels = new float[]{0, 2, 4, 6};
        } else if (maxHours <= 9) {
            maxYValue = 9f;
            yAxisLabels = new float[]{0, 3, 6, 9};
        } else if (maxHours <= 12) {
            maxYValue = 12f;
            yAxisLabels = new float[]{0, 3, 6, 9, 12};
        } else if (maxHours <= 18) {
            maxYValue = 18f;
            yAxisLabels = new float[]{0, 6, 12, 18};
        } else if (maxHours <= 24) {
            maxYValue = 24f;
            yAxisLabels = new float[]{0, 6, 12, 18, 24};
        } else {
            int step = maxHours <= 36 ? 6 : 12;
            int count = (int) Math.ceil(maxHours / step);
            maxYValue = count * step;
            yAxisLabels = new float[count + 1];
            for (int i = 0; i <= count; i++) {
                yAxisLabels[i] = i * step;
            }
        }
        updatePaddings();
    }

    public void setData(List<DayData> data) {
        this.dayDataList = data != null ? data : new ArrayList<>();
        this.totalMinutes = 0;
        
        // 找出最大的单日时长（小时）
        float maxDayHours = 0;
        for (DayData day : dayDataList) {
            totalMinutes += day.minutes;
            float hours = day.minutes / 60f;
            if (hours > maxDayHours) {
                maxDayHours = hours;
            }
        }
        
        // 根据最大时长动态计算纵坐标刻度
        calculateYAxisLabels(maxDayHours);
        
        invalidate();
    }

    public int getTotalMinutes() {
        return totalMinutes;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureDefaultHeight(widthMeasureSpec, heightMeasureSpec, 240);
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
        String prefix = "本周总时长：";

        Paint prefixPaint = new Paint();
        prefixPaint.setColor(Color.parseColor("#5A7D5A"));
        prefixPaint.setTextSize(spToPx(14));
        prefixPaint.setFakeBoldText(true);
        prefixPaint.setAntiAlias(true);
        prefixPaint.setTextAlign(Paint.Align.CENTER);

        float prefixY = dpToPx(24);
        canvas.drawText(prefix, getWidth() / 2f, prefixY, prefixPaint);

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

        float baselineY = dpToPx(80);
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
        if (yAxisLabels == null) return;
        for (float yLabel : yAxisLabels) {
            float y = paddingTop + ((maxYValue - yLabel) / maxYValue) * chartHeight;
            canvas.drawLine(paddingLeft, y, getWidth() - paddingRight, y, gridPaint);
        }
    }

    private void drawYAxisLabels(Canvas canvas) {
        if (yAxisLabels == null) return;
        
        Paint labelPaint = new Paint();
        labelPaint.setColor(Color.parseColor("#666666"));
        labelPaint.setTextSize(spToPx(11));
        labelPaint.setAntiAlias(true);
        labelPaint.setTextAlign(Paint.Align.RIGHT);

        for (float yLabel : yAxisLabels) {
            float y = paddingTop + ((maxYValue - yLabel) / maxYValue) * chartHeight;
            canvas.drawText(String.valueOf((int) yLabel) + "h", paddingLeft - dpToPx(6), y + dpToPx(4), labelPaint);
        }
    }

    private void drawBars(Canvas canvas) {
        if (dayDataList.isEmpty()) return;

        int barWidth = dpToPx(24);
        int barGap = dpToPx(8);
        int pressExpand = dpToPx(PRESS_EXPAND_DP);
        int totalBarWidth = dayDataList.size() * barWidth + (dayDataList.size() - 1) * barGap;
        int startX = paddingLeft + (chartWidth - totalBarWidth) / 2;

        barBoundsList.clear();

        for (int i = 0; i < dayDataList.size(); i++) {
            DayData data = dayDataList.get(i);
            float barHeight = (data.minutes / 60f / maxYValue) * chartHeight;
            
            float left, top, right, bottom;
            Paint paint;
            
            // 判断当前柱子是否被按下
            if (touchedBarIndex == i) {
                // 按下状态：扩展大小，使用深色
                left = startX + i * (barWidth + barGap) - pressExpand;
                top = paddingTop + chartHeight - barHeight - pressExpand;
                right = left + barWidth + pressExpand * 2;
                bottom = paddingTop + chartHeight + pressExpand;
                paint = pressedBarPaint;
            } else {
                // 正常状态
                left = startX + i * (barWidth + barGap);
                top = paddingTop + chartHeight - barHeight;
                right = left + barWidth;
                bottom = paddingTop + chartHeight;
                paint = barPaint;
            }

            RectF barBounds = new RectF(startX + i * (barWidth + barGap), paddingTop, 
                    startX + i * (barWidth + barGap) + barWidth, bottom);
            barBoundsList.add(barBounds);

            if (barHeight > 0) {
                float radius = (touchedBarIndex == i ? barWidth + pressExpand * 2 : barWidth) / 2f;
                drawRoundTopBar(canvas, left, top, right, bottom, radius, paint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getAction();
        float x = event.getX();
        float y = event.getY();

        // 查找当前点击位置对应的柱子索引，并且只对有数据的柱子进行检测
        int clickedBarIndex = -1;
        for (int i = 0; i < barBoundsList.size(); i++) {
            RectF bounds = barBoundsList.get(i);
            DayData data = dayDataList.get(i);
            // 只有有数据的柱子才能被点击
            if (bounds.contains(x, y) && data.minutes > 0) {
                clickedBarIndex = i;
                break;
            }
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (clickedBarIndex != -1) {
                    touchedBarIndex = clickedBarIndex;
                    invalidate(); // 重绘显示按下状态
                    return true; // 消费这个事件，继续接收后续事件
                }
                break;

            case MotionEvent.ACTION_MOVE:
                // 如果手指移出了当前按下的柱子，恢复原状
                if (touchedBarIndex != -1 && touchedBarIndex != clickedBarIndex) {
                    touchedBarIndex = -1;
                    invalidate();
                }
                break;

            case MotionEvent.ACTION_UP:
                if (touchedBarIndex != -1 && clickedBarIndex == touchedBarIndex) {
                    // 手指在同一根柱子上抬起，执行回调（确保该柱子有数据）
                    final int finalIndex = touchedBarIndex;
                    final DayData finalData = dayDataList.get(finalIndex);
                    
                    // 先恢复原状
                    touchedBarIndex = -1;
                    invalidate();
                    
                    // 只有有数据才执行回调
                    if (finalData.minutes > 0) {
                        // 延迟一小段时间后执行回调，确保交互效果完成
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (onBarClickListener != null) {
                                    onBarClickListener.onBarClick(finalData, finalIndex);
                                }
                            }
                        }, 100);
                    }
                } else {
                    touchedBarIndex = -1;
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_CANCEL:
                // 取消事件，恢复原状
                touchedBarIndex = -1;
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }

    private void drawRoundTopBar(Canvas canvas, float left, float top, float right, float bottom, float radius, Paint paint) {
        Path path = new Path();
        path.moveTo(left, bottom);
        path.lineTo(left, top + radius);
        path.quadTo(left, top, left + radius, top);
        path.lineTo(right - radius, top);
        path.quadTo(right, top, right, top + radius);
        path.lineTo(right, bottom);
        path.close();
        canvas.drawPath(path, paint);
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
}

