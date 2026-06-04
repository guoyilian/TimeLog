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

public class WeekBarChartView extends View {

    private float[] yAxisLabels;
    private float maxYValue = 12f;

    private Paint barPaint;
    private Paint gridPaint;
    private Paint textPaint;

    private List<DayData> dayDataList = new ArrayList<>();
    private List<RectF> barBoundsList = new ArrayList<>();
    private int totalMinutes = 0;

    private int paddingLeft;
    private int paddingRight;
    private int paddingTop;
    private int paddingBottom;

    private int chartWidth;
    private int chartHeight;

    private OnBarClickListener onBarClickListener;

    // 点击交互效果相关
    private int touchedBarIndex = -1; // 记录当前按下的柱子索引
    private Paint pressedBarPaint; // 按下状态的画笔
    private static final int PRESS_EXPAND_DP = 3; // 按下时柱子的扩展大小

    public interface OnBarClickListener {
        void onBarClick(DayData dayData, int index);
    }

    public static class DayData {
        public String dayOfWeek;
        public String date;
        public String fullDate; // 完整日期字符串 yyyy-MM-dd
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

    public void setOnBarClickListener(OnBarClickListener listener) {
        this.onBarClickListener = listener;
    }

    public void clearTouchState() {
        touchedBarIndex = -1;
        invalidate();
    }

    private void init() {
        barPaint = new Paint();
        barPaint.setColor(Color.parseColor("#6A9974"));
        barPaint.setStyle(Paint.Style.FILL);
        barPaint.setAntiAlias(true);

        pressedBarPaint = new Paint();
        pressedBarPaint.setColor(Color.parseColor("#4A7D5A")); // 更深的颜色
        pressedBarPaint.setStyle(Paint.Style.FILL);
        pressedBarPaint.setAntiAlias(true);

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

        // 初始化默认的纵坐标刻度
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
        paddingTop = dpToPx(46);
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

        // 查找当前点击位置对应的柱子索引
        int clickedBarIndex = -1;
        for (int i = 0; i < barBoundsList.size(); i++) {
            RectF bounds = barBoundsList.get(i);
            if (bounds.contains(x, y)) {
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
                    // 手指在同一根柱子上抬起，执行回调
                    final int finalIndex = touchedBarIndex;
                    final DayData finalData = dayDataList.get(finalIndex);
                    
                    // 先恢复原状
                    touchedBarIndex = -1;
                    invalidate();
                    
                    // 延迟一小段时间后执行回调，确保交互效果完成
                    postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (onBarClickListener != null) {
                                onBarClickListener.onBarClick(finalData, finalIndex);
                            }
                        }
                    }, 100);
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

    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private int spToPx(int sp) {
        return (int) (sp * getResources().getDisplayMetrics().scaledDensity);
    }
}

