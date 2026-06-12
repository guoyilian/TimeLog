package com.example.timer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class MonthHeatmapView extends BaseChartView {

    public interface OnCellClickListener {
        void onCellClick(String date, int day, int minutes);
    }

    private static final int GRID_COLUMNS = 7;
    private static final int GRID_ROWS = 6;
    private static final int MAX_DAYS = 31;
    private static final long RIPPLE_DURATION_MS = 2000L;
    private static final long HIGHLIGHT_DURATION_MS = 500L;

    private final int[] dailyMinutes = new int[MAX_DAYS];
    private final List<CellData> cells = new ArrayList<>();

    private Paint titlePaint;
    private Paint weekdayPaint;
    private Paint dayNumberPaint;
    private Paint leftDayPaint;
    private Paint cellPaint;
    private Paint highlightPaint;
    private Paint ripplePaint;

    private int year;
    private int month;
    private int daysInMonth;
    private int firstDayColumn;
    private int totalMinutes;

    private float cellSize;
    private float gridStartX;
    private float gridStartY;
    private float cellGap;
    private float cellCornerRadius;

    private OnCellClickListener cellClickListener;

    private int highlightRow;
    private int highlightCol;
    private float highlightAlpha;
    private ValueAnimator highlightAnimator;

    private int rippleRow;
    private int rippleCol;
    private float rippleProgress;
    private ValueAnimator rippleAnimator;

    private int selectedRow;
    private int selectedCol;

    private static class CellData {
        int row;
        int col;
        int day;
        int minutes;
        String date;
        RectF bounds;

        CellData(int row, int col, int day, int minutes, String date, RectF bounds) {
            this.row = row;
            this.col = col;
            this.day = day;
            this.minutes = minutes;
            this.date = date;
            this.bounds = bounds;
        }
    }

    public MonthHeatmapView(Context context) {
        super(context);
    }

    public MonthHeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public MonthHeatmapView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void init(AttributeSet attrs) {
        highlightRow = -1;
        highlightCol = -1;
        highlightAlpha = 0f;
        rippleRow = -1;
        rippleCol = -1;
        rippleProgress = 0f;
        selectedRow = -1;
        selectedCol = -1;

        setClickable(true);

        titlePaint = createPaint();
        titlePaint.setColor(Color.parseColor("#5A7D5A"));
        titlePaint.setTextSize(spToPx(14));
        titlePaint.setFakeBoldText(true);
        titlePaint.setTextAlign(Paint.Align.CENTER);

        weekdayPaint = createPaint();
        weekdayPaint.setColor(Color.parseColor("#666666"));
        weekdayPaint.setTextSize(spToPx(12));
        weekdayPaint.setTextAlign(Paint.Align.CENTER);

        dayNumberPaint = createPaint();
        dayNumberPaint.setColor(Color.parseColor("#333333"));
        dayNumberPaint.setTextSize(spToPx(11));
        dayNumberPaint.setTextAlign(Paint.Align.CENTER);

        leftDayPaint = createPaint();
        leftDayPaint.setColor(Color.parseColor("#999999"));
        leftDayPaint.setTextSize(spToPx(10));
        leftDayPaint.setTextAlign(Paint.Align.RIGHT);

        cellPaint = createPaint();
        cellPaint.setStyle(Paint.Style.FILL);

        highlightPaint = createPaint();
        highlightPaint.setStyle(Paint.Style.FILL);
        highlightPaint.setColor(Color.parseColor("#40000000"));

        ripplePaint = createPaint();
        ripplePaint.setStyle(Paint.Style.FILL);
        ripplePaint.setColor(Color.parseColor("#8EB88E"));

        cellGap = dpToPx(3);
        cellCornerRadius = dpToPx(6);

        Calendar now = Calendar.getInstance();
        year = now.get(Calendar.YEAR);
        month = now.get(Calendar.MONTH) + 1;
        daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH);
        firstDayColumn = now.get(Calendar.DAY_OF_WEEK) - 1;
    }

    public void setOnCellClickListener(OnCellClickListener listener) {
        this.cellClickListener = listener;
    }

    public void setMonthData(int[] dailyMinutes, int firstDayOfWeek, int year, int month) {
        this.year = year;
        this.month = month;
        this.firstDayColumn = firstDayOfWeek;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month - 1, 1);
        this.daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH);

        for (int i = 0; i < MAX_DAYS; i++) {
            this.dailyMinutes[i] = (dailyMinutes != null && i < dailyMinutes.length) ? dailyMinutes[i] : 0;
        }

        totalMinutes = 0;
        for (int day = 1; day <= daysInMonth; day++) {
            totalMinutes += this.dailyMinutes[day - 1];
        }

        cells.clear();
        requestLayout();
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, heightSize);
        } else {
            int defaultHeight = getResources().getDimensionPixelSize(R.dimen.heatmap_chart_height);
            setMeasuredDimension(width, defaultHeight);
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        calculateGrid();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAllAnimations();
    }

    private void calculateGrid() {
        int viewWidth = getWidth();
        int viewHeight = getHeight();

        if (viewWidth <= 0 || viewHeight <= 0) {
            return;
        }

        float leftDayWidth = dpToPx(24);

        float contentWidth = viewWidth - leftDayWidth - dpToPx(6);
        if (contentWidth <= 0) return;

        float cellSizeByWidth = (contentWidth - cellGap * (GRID_COLUMNS - 1)) / GRID_COLUMNS;

        Paint.FontMetrics weekdayMetrics = weekdayPaint.getFontMetrics();
        float weekdayBlockHeight = -weekdayMetrics.ascent + weekdayMetrics.descent;
        float minGridAreaTop = dpToPx(115) + weekdayBlockHeight + cellGap;
        float gridAreaBottom = viewHeight - dpToPx(2);
        float availableGridHeight = gridAreaBottom - minGridAreaTop;
        if (availableGridHeight <= 0) return;

        float cellSizeByHeight = (availableGridHeight - cellGap * (GRID_ROWS - 1)) / GRID_ROWS;

        cellSize = Math.min(cellSizeByWidth, cellSizeByHeight);
        if (cellSize <= 0) return;

        float gridWidth = GRID_COLUMNS * cellSize + cellGap * (GRID_COLUMNS - 1);
        float actualGridHeight = GRID_ROWS * cellSize + cellGap * (GRID_ROWS - 1);
        gridStartX = (viewWidth - gridWidth) / 2f;
        gridStartY = gridAreaBottom - actualGridHeight;

        cells.clear();
        int day = 1;
        for (int row = 0; row < GRID_ROWS; row++) {
            for (int col = 0; col < GRID_COLUMNS; col++) {
                float left = gridStartX + col * (cellSize + cellGap);
                float top = gridStartY + row * (cellSize + cellGap);
                RectF bounds = new RectF(left, top, left + cellSize, top + cellSize);

                if ((row == 0 && col < firstDayColumn) || day > daysInMonth) {
                    cells.add(new CellData(row, col, 0, -1, null, bounds));
                    continue;
                }

                int minutes = dailyMinutes[day - 1];
                String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month, day);
                cells.add(new CellData(row, col, day, minutes, date, bounds));
                day++;
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (cells.isEmpty()) {
            calculateGrid();
        }

        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        String prefix = year + "年" + month + "月 总时长：";

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

        String[] weekdays = {"日", "一", "二", "三", "四", "五", "六"};

        Paint.FontMetrics weekdayMetrics = weekdayPaint.getFontMetrics();
        float weekdayBaseline = gridStartY - cellGap - weekdayMetrics.descent;

        for (int col = 0; col < GRID_COLUMNS; col++) {
            float centerX = gridStartX + col * (cellSize + cellGap) + cellSize / 2f;
            canvas.drawText(weekdays[col], centerX, weekdayBaseline, weekdayPaint);
        }

        drawLeftDayNumbers(canvas);

        for (CellData cell : cells) {
            boolean isSelected = cell.row == selectedRow && cell.col == selectedCol;
            
            if (isSelected) {
                cellPaint.setColor(Color.BLACK);
            } else {
                int baseColor = getColorForMinutes(cell.minutes);
                cellPaint.setColor(applyHighlight(cell, baseColor));
            }
            canvas.drawRoundRect(cell.bounds, cellCornerRadius, cellCornerRadius, cellPaint);

            if (cell.day > 0) {
                float centerX = cell.bounds.centerX();
                float centerY = cell.bounds.centerY();
                Paint.FontMetrics dayMetrics = dayNumberPaint.getFontMetrics();
                float dayBaseline = centerY - dayMetrics.ascent / 2 - dayMetrics.descent / 2;
                
                if (isSelected) {
                    dayNumberPaint.setColor(Color.WHITE);
                } else {
                    dayNumberPaint.setColor(Color.parseColor("#333333"));
                }
                canvas.drawText(String.valueOf(cell.day), centerX, dayBaseline, dayNumberPaint);
            }

            if (rippleRow == cell.row && rippleCol == cell.col && rippleProgress > 0f) {
                drawRipple(canvas, cell);
            }
        }
    }

    private int applyHighlight(CellData cell, int baseColor) {
        if (highlightRow == cell.row && highlightCol == cell.col && highlightAlpha > 0f) {
            int highlightColor = highlightPaint.getColor();
            int r = (baseColor >> 16) & 0xFF;
            int g = (baseColor >> 8) & 0xFF;
            int b = baseColor & 0xFF;
            int hr = (highlightColor >> 16) & 0xFF;
            int hg = (highlightColor >> 8) & 0xFF;
            int hb = highlightColor & 0xFF;
            int a = (int) (highlightAlpha * 100);
            int newR = Math.min(255, (r * (100 - a) + hr * a) / 100);
            int newG = Math.min(255, (g * (100 - a) + hg * a) / 100);
            int newB = Math.min(255, (b * (100 - a) + hb * a) / 100);
            return Color.argb(255, newR, newG, newB);
        }
        return baseColor;
    }

    private void drawLeftDayNumbers(Canvas canvas) {
        int day = 1;
        float leftX = gridStartX - dpToPx(6);

        for (int row = 0; row < GRID_ROWS; row++) {
            float cellTop = gridStartY + row * (cellSize + cellGap);
            float cellCenterY = cellTop + cellSize / 2f;

            int firstDayInRow = -1;
            for (int col = 0; col < GRID_COLUMNS; col++) {
                if ((row == 0 && col < firstDayColumn) || day > daysInMonth) {
                    continue;
                }
                if (firstDayInRow == -1) {
                    firstDayInRow = day;
                }
                day++;
            }

            if (firstDayInRow > 0) {
                Paint.FontMetrics metrics = leftDayPaint.getFontMetrics();
                float baseline = cellCenterY - metrics.ascent / 2 - metrics.descent / 2;
                canvas.drawText(String.valueOf(firstDayInRow), leftX, baseline, leftDayPaint);
            }
        }
    }

    private void drawRipple(Canvas canvas, CellData cell) {
        canvas.save();

        Path clipPath = new Path();
        clipPath.addRoundRect(cell.bounds, cellCornerRadius, cellCornerRadius, Path.Direction.CW);
        canvas.clipPath(clipPath);

        float maxRadius = (float) Math.hypot(cell.bounds.width(), cell.bounds.height());
        float radius = maxRadius * rippleProgress;
        int alpha = (int) (80 * (1f - rippleProgress));
        ripplePaint.setAlpha(Math.max(alpha, 0));
        float centerX = cell.bounds.centerX();
        float centerY = cell.bounds.centerY();
        canvas.drawCircle(centerX, centerY, radius, ripplePaint);
        ripplePaint.setAlpha(255);

        canvas.restore();
    }

    private int getColorForMinutes(int minutes) {
        if (minutes < 0) {
            return Color.TRANSPARENT;
        }
        if (minutes == 0) {
            return Color.parseColor("#F0F4F0");
        }
        if (minutes <= 60) {
            return Color.parseColor("#D0E8D0");
        }
        if (minutes <= 120) {
            return Color.parseColor("#A8D5A8");
        }
        if (minutes <= 240) {
            return Color.parseColor("#78B878");
        }
        if (minutes <= 480) {
            return Color.parseColor("#4A9A4A");
        }
        return Color.parseColor("#2D7A2D");
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                CellData cell = findCellAt(x, y);
                if (cell != null && cell.day > 0) {
                    highlightRow = cell.row;
                    highlightCol = cell.col;
                    rippleRow = cell.row;
                    rippleCol = cell.col;
                    startHighlightAnimation();
                    startRippleAnimation();
                    invalidate();
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
                CellData touchedCell = findCellAt(x, y);
                if (touchedCell != null && touchedCell.day > 0 &&
                    touchedCell.row == rippleRow && touchedCell.col == rippleCol) {
                    selectedRow = touchedCell.row;
                    selectedCol = touchedCell.col;
                    if (cellClickListener != null) {
                        cellClickListener.onCellClick(touchedCell.date, touchedCell.day, touchedCell.minutes);
                    }
                    performClick();
                }
                clearTouchState();
                return true;

            case MotionEvent.ACTION_CANCEL:
                clearTouchState();
                return true;

            case MotionEvent.ACTION_MOVE:
                CellData moveCell = findCellAt(x, y);
                if (moveCell == null || moveCell.row != rippleRow || moveCell.col != rippleCol) {
                    clearTouchState();
                }
                return true;
        }
        return super.onTouchEvent(event);
    }

    private void clearTouchState() {
        highlightRow = -1;
        highlightCol = -1;
        rippleRow = -1;
        rippleCol = -1;
        stopAllAnimations();
        invalidate();
    }

    public void clearSelectedState() {
        selectedRow = -1;
        selectedCol = -1;
        invalidate();
    }

    private CellData findCellAt(float x, float y) {
        if (cells.isEmpty()) {
            return null;
        }
        for (CellData cell : cells) {
            if (cell.bounds.contains(x, y)) {
                return cell;
            }
        }
        return null;
    }

    private void startHighlightAnimation() {
        stopHighlightAnimation();
        highlightAlpha = 0.4f;
        highlightAnimator = ValueAnimator.ofFloat(0.4f, 0f);
        highlightAnimator.setDuration(HIGHLIGHT_DURATION_MS);
        highlightAnimator.setInterpolator(new DecelerateInterpolator());
        highlightAnimator.addUpdateListener(animation -> {
            highlightAlpha = (float) animation.getAnimatedValue();
            invalidate();
        });
        highlightAnimator.start();
    }

    private void startRippleAnimation() {
        stopRippleAnimation();
        rippleProgress = 0f;
        rippleAnimator = ValueAnimator.ofFloat(0f, 1f);
        rippleAnimator.setDuration(RIPPLE_DURATION_MS);
        rippleAnimator.setInterpolator(new DecelerateInterpolator());
        rippleAnimator.addUpdateListener(animation -> {
            rippleProgress = (float) animation.getAnimatedValue();
            invalidate();
        });
        rippleAnimator.start();
    }

    private void stopRippleAnimation() {
        if (rippleAnimator != null && rippleAnimator.isRunning()) {
            rippleAnimator.cancel();
        }
        rippleAnimator = null;
        rippleProgress = 0f;
    }

    private void stopHighlightAnimation() {
        if (highlightAnimator != null && highlightAnimator.isRunning()) {
            highlightAnimator.cancel();
        }
        highlightAnimator = null;
        highlightAlpha = 0f;
    }

    private void stopAllAnimations() {
        stopRippleAnimation();
        stopHighlightAnimation();
    }
}