package com.example.timer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class YearHeatmapView extends BaseChartView {
    public interface OnMonthClickListener {
        void onMonthClick(int year, int month);
    }

    private Paint textPaint;
    private Paint cellPaint;
    private Paint disabledCellPaint;
    private Paint highlightPaint;
    private Paint borderHighlightPaint;

    private int currentYear;
    private List<TimerRecord> allRecords;
    private OnMonthClickListener monthClickListener;

    private List<MonthRect> monthRects;

    private int clickedMonth;
    private float clickScale;
    private ValueAnimator clickAnimator;
    private boolean isPressed;

    private String[] monthNames;

    private static class MonthRect {
        int month;
        RectF rect;
        boolean isClickable;

        MonthRect(int month, RectF rect, boolean isClickable) {
            this.month = month;
            this.rect = rect;
            this.isClickable = isClickable;
        }
    }

    public YearHeatmapView(Context context) {
        super(context);
    }

    public YearHeatmapView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(AttributeSet attrs) {
        monthRects = new ArrayList<>();
        clickedMonth = -1;
        clickScale = 1f;
        isPressed = false;
        monthNames = new String[]{"1 月", "2 月", "3 月", "4 月", "5 月", "6 月",
                                   "7 月", "8 月", "9 月", "10 月", "11 月", "12 月"};

        textPaint = createPaint();
        textPaint.setColor(Color.parseColor("#333333"));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);

        cellPaint = createPaint();
        cellPaint.setStyle(Paint.Style.FILL);

        disabledCellPaint = createPaint();
        disabledCellPaint.setColor(Color.parseColor("#F5F5F5"));
        disabledCellPaint.setStyle(Paint.Style.FILL);

        highlightPaint = createPaint();
        highlightPaint.setColor(Color.parseColor("#8EB88E"));
        highlightPaint.setStyle(Paint.Style.FILL);

        borderHighlightPaint = createPaint();
        borderHighlightPaint.setColor(Color.parseColor("#4A9A6A"));
        borderHighlightPaint.setStyle(Paint.Style.STROKE);
        borderHighlightPaint.setStrokeWidth(dpToPx(3));

        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);

        setClickable(true);
    }

    public void setOnMonthClickListener(OnMonthClickListener listener) {
        this.monthClickListener = listener;
    }

    public void setYearData(int year, List<TimerRecord> records) {
        this.currentYear = year;
        this.allRecords = records;
        invalidate();
    }
    
    private boolean isWithinUsageRange(int month) {
        if (allRecords == null || allRecords.isEmpty()) return false;
        
        // 获取最早记录时间
        long earliestRecord = Long.MAX_VALUE;
        for (TimerRecord record : allRecords) {
            if (record.getStart() < earliestRecord) {
                earliestRecord = record.getStart();
            }
        }
        
        Calendar earliestCal = Calendar.getInstance();
        earliestCal.setTimeInMillis(earliestRecord);
        int earliestYear = earliestCal.get(Calendar.YEAR);
        int earliestMonth = earliestCal.get(Calendar.MONTH);
        
        // 检查是否在使用范围内（包括最早使用月份之后的所有月份）
        if (currentYear > earliestYear || (currentYear == earliestYear && month >= earliestMonth)) {
            return true;
        }
        return false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);

        int horizontalPadding = width / 32;
        int availableWidth = width - horizontalPadding * 2;

        int colCount = 3;
        int monthSpacing = width / 32;
        int monthWidth = (availableWidth - monthSpacing * (colCount - 1)) / colCount;
        int cellGap = Math.max(1, monthWidth / 60);
        int cellSize = (monthWidth - cellGap * 6) / 7;
        cellSize = Math.max(30, Math.min(64, cellSize));

        int textSize = (int) Math.max(16, Math.min(40, monthWidth / 8.5));

        int titleBottomMargin = cellGap * 4;
        int gridHeight = 6 * (cellSize + cellGap);
        int monthHeight = textSize + titleBottomMargin + gridHeight;

        int rowSpacing = (int) (monthSpacing * 1.5f);
        int verticalPadding = rowSpacing;
        int rowCount = 4;
        int totalHeight = verticalPadding * 2 + monthHeight * rowCount + rowSpacing * (rowCount - 1);

        setMeasuredDimension(width, totalHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();
        Calendar today = Calendar.getInstance();

        int horizontalPadding = width / 32;
        int availableWidth = width - horizontalPadding * 2;

        int colCount = 3;
        int monthSpacing = width / 32;
        int monthWidth = (availableWidth - monthSpacing * (colCount - 1)) / colCount;
        int cellGap = Math.max(1, monthWidth / 60);
        int cellSize = (monthWidth - cellGap * 6) / 7;
        cellSize = Math.max(30, Math.min(64, cellSize));

        int cornerRadius = cellSize / 3;

        int textSize = (int) Math.max(16, Math.min(40, monthWidth / 8.5));
        textPaint.setTextSize(textSize);

        int titleBottomMargin = cellGap * 4;
        int gridHeight = 6 * (cellSize + cellGap);
        int monthHeight = textSize + titleBottomMargin + gridHeight;

        int rowSpacing = (int) (monthSpacing * 1.5f);
        int verticalPadding = rowSpacing;

        monthRects.clear();

        for (int month = 0; month < 12; month++) {
            int col = month % 3;
            int row = month / 3;

            int monthX = horizontalPadding + col * (monthWidth + monthSpacing);
            int monthY = verticalPadding + row * (monthHeight + rowSpacing);

            boolean isFutureMonth = isFutureMonth(month, today);
            boolean isCurrentMonth = !isFutureMonth && month == today.get(Calendar.MONTH);
            boolean isWithinUsageRange = isWithinUsageRange(month);

            Paint monthTextPaint = textPaint;
            if (isFutureMonth || !isWithinUsageRange) {
                monthTextPaint = new Paint(textPaint);
                monthTextPaint.setColor(Color.parseColor("#999999"));
            } else if (isCurrentMonth) {
                monthTextPaint = new Paint(textPaint);
                monthTextPaint.setColor(Color.parseColor("#4A9A6A"));
            }

            int titleX = monthX + monthWidth / 2;
            int titleY = monthY + textSize;
            canvas.drawText(monthNames[month], titleX, titleY, monthTextPaint);

            int gridAreaX = monthX + (monthWidth - 7 * cellSize - 6 * cellGap) / 2;
            int gridStartY = monthY + textSize + titleBottomMargin;

            Calendar calendar = Calendar.getInstance();
            calendar.set(currentYear, month, 1);
            int firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1;
            int daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH);

            RectF monthRect = new RectF(monthX, monthY, monthX + monthWidth, monthY + monthHeight);
            boolean isClickable = isWithinUsageRange(month) && !isFutureMonth;
            monthRects.add(new MonthRect(month, monthRect, isClickable));

            for (int day = 0; day < daysInMonth; day++) {
                int gridX = gridAreaX + ((day + firstDayOfWeek) % 7) * (cellSize + cellGap);
                int gridY = gridStartY + ((day + firstDayOfWeek) / 7) * (cellSize + cellGap);

                if (isFutureMonth) {
                    cellPaint.setColor(Color.parseColor("#E8E8E8"));
                } else {
                    int dayMinutes = getDayDuration(month, day + 1);
                    cellPaint.setColor(getColorForDuration(dayMinutes));
                }

                canvas.drawRoundRect(gridX, gridY, gridX + cellSize, gridY + cellSize,
                                    cornerRadius, cornerRadius, cellPaint);
            }

            if (clickedMonth == month) {
                float scale = clickScale;
                float centerX = monthX + monthWidth / 2f;
                float centerY = monthY + monthHeight / 2f;
                float scaledWidth = monthWidth * scale;
                float scaledHeight = monthHeight * scale;
                float left = centerX - scaledWidth / 2f;
                float top = centerY - scaledHeight / 2f;

                int alpha = (int) ((1f - scale) * 120);
                if (alpha < 30) alpha = 30;
                highlightPaint.setAlpha(alpha);
                canvas.drawRoundRect(left, top, left + scaledWidth, top + scaledHeight,
                                    cornerRadius, cornerRadius, highlightPaint);

                borderHighlightPaint.setAlpha(alpha);
                canvas.drawRoundRect(left, top, left + scaledWidth, top + scaledHeight,
                                    cornerRadius, cornerRadius, borderHighlightPaint);
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();
        int action = event.getAction();

        if (action == MotionEvent.ACTION_DOWN) {
            for (MonthRect monthRect : monthRects) {
                if (monthRect.isClickable && monthRect.rect.contains(x, y)) {
                    clickedMonth = monthRect.month;
                    isPressed = true;
                    startClickAnimation();
                    return true;
                }
            }
        } else if (action == MotionEvent.ACTION_UP) {
            if (clickedMonth >= 0) {
                for (MonthRect monthRect : monthRects) {
                    if (monthRect.month == clickedMonth && monthRect.rect.contains(x, y)) {
                        if (monthClickListener != null) {
                            monthClickListener.onMonthClick(currentYear, clickedMonth);
                        }
                        break;
                    }
                }
                clickedMonth = -1;
                isPressed = false;
                invalidate();
            }
        } else if (action == MotionEvent.ACTION_CANCEL) {
            clickedMonth = -1;
            isPressed = false;
            invalidate();
        }

        return true;
    }

    private void startClickAnimation() {
        if (clickAnimator != null) {
            clickAnimator.cancel();
        }

        clickAnimator = ValueAnimator.ofFloat(1f, 0.92f);
        clickAnimator.setDuration(200);
        clickAnimator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        clickAnimator.addUpdateListener(animation -> {
            clickScale = (float) animation.getAnimatedValue();
            invalidate();
        });
        clickAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                ValueAnimator restoreAnimator = ValueAnimator.ofFloat(0.92f, 1f);
                restoreAnimator.setDuration(150);
                restoreAnimator.setInterpolator(new android.view.animation.AccelerateInterpolator());
                restoreAnimator.addUpdateListener(anim -> {
                    clickScale = (float) anim.getAnimatedValue();
                    invalidate();
                });
                restoreAnimator.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator anim) {
                        clickScale = 1f;
                        invalidate();
                    }
                });
                restoreAnimator.start();
            }
        });
        clickAnimator.start();
    }

    private boolean isFutureMonth(int month, Calendar today) {
        return currentYear > today.get(Calendar.YEAR) ||
               (currentYear == today.get(Calendar.YEAR) && month > today.get(Calendar.MONTH));
    }

    private int getDayDuration(int month, int day) {
        if (allRecords == null || allRecords.isEmpty()) {
            return 0;
        }

        long targetDayStart = DateUtils.getSpecificDayStartMillis(currentYear, month, day);
        long targetDayEnd = targetDayStart + 24L * 60 * 60 * 1000;
        int totalMinutes = 0;

        for (TimerRecord record : allRecords) {
            long start = record.getStart();
            if (start >= targetDayStart && start < targetDayEnd) {
                totalMinutes += record.getDurationMin();
            }
        }

        return totalMinutes;
    }

    private int getColorForDuration(int minutes) {
        if (minutes == 0) {
            return Color.parseColor("#E8E8E8");
        } else if (minutes <= 60) {
            return Color.parseColor("#D0E8D0");
        } else if (minutes <= 120) {
            return Color.parseColor("#A8D5A8");
        } else if (minutes <= 240) {
            return Color.parseColor("#78B878");
        } else if (minutes <= 480) {
            return Color.parseColor("#4A9A4A");
        } else {
            return Color.parseColor("#2D7A2D");
        }
    }
}