package com.example.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Calendar;
import java.util.List;

public class YearStatisticsView extends LinearLayout {
    private YearHeatmapView yearHeatmap;
    private TextView yearTitle;
    private TextView yearPrev;
    private TextView yearNext;
    private TextView totalDays;
    private TextView totalHours;
    private View btnExport;
    private View btnImport;
    private YearHeatmapView.OnMonthClickListener monthClickListener;
    private OnExportClickListener exportClickListener;
    private OnImportClickListener importClickListener;

    private int currentYear;
    private int minYear;
    private int maxYear;
    private List<TimerRecord> allRecords;

    public interface OnExportClickListener {
        void onExportClick();
    }

    public interface OnImportClickListener {
        void onImportClick();
    }

    public YearStatisticsView(Context context) {
        super(context);
        init(context);
    }

    public YearStatisticsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        LayoutInflater.from(context).inflate(R.layout.view_year_statistics, this, true);

        yearHeatmap = findViewById(R.id.year_heatmap);
        yearTitle = findViewById(R.id.year_title);
        yearPrev = findViewById(R.id.year_prev);
        yearNext = findViewById(R.id.year_next);
        totalDays = findViewById(R.id.total_days);
        totalHours = findViewById(R.id.total_hours);

        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
        maxYear = currentYear;
        minYear = currentYear;
        yearTitle.setText(currentYear + "年 · 年度总览");

        if (yearPrev != null) {
            yearPrev.setOnClickListener(v -> navigateYear(-1));
        }
        if (yearNext != null) {
            yearNext.setOnClickListener(v -> navigateYear(1));
        }

        yearHeatmap.setOnMonthClickListener((year, month) -> {
            if (monthClickListener != null) {
                // 检查是否在实际数据范围内
                boolean isInUsageRange = isWithinUsageRange(year, month);
                if (isInUsageRange) {
                    // 检查是否是未来月份
                    Calendar currentCal = Calendar.getInstance();
                    int currentYear = currentCal.get(Calendar.YEAR);
                    int currentMonth = currentCal.get(Calendar.MONTH);
                    
                    if (!(year > currentYear || (year == currentYear && month > currentMonth))) {
                        // 实际数据范围内且不是未来月份，允许跳转
                        monthClickListener.onMonthClick(year, month);
                    }
                }
                // 其他情况无响应，不显示Toast
            }
        });

        btnExport = findViewById(R.id.btn_export);
        btnImport = findViewById(R.id.btn_import);
        View layoutBackupInfo = findViewById(R.id.layout_backup_info);

        if (btnExport != null) {
            btnExport.setOnClickListener(v -> {
                if (exportClickListener != null) {
                    exportClickListener.onExportClick();
                }
            });
        }

        if (btnImport != null) {
            btnImport.setOnClickListener(v -> {
                if (importClickListener != null) {
                    importClickListener.onImportClick();
                }
            });
        }

        if (layoutBackupInfo != null) {
            layoutBackupInfo.setOnClickListener(v -> {
                String message = "📌 为什么要备份数据？\n" +
                        "您的计时数据保存在应用内部，卸载应用、清除应用数据、或手机出现故障时，数据都会丢失。\n\n" +
                        "定期备份可以保护您的时间记录不被意外清除。\n\n" +
                        "📁 导出数据\n" +
                        "• 导出的文件会保存到：手机/下载/TimerBackup 文件夹\n" +
                        "• 每次导出都会生成新文件，不会覆盖之前的备份\n" +
                        "• 建议每月或重要记录后都建议导出备份\n\n" +
                        "• 备份文件可在手机\"文件管理\"→\"下载\"→\"TimerBackup\"中找到\n\n" +
                        "📂 导入数据\n" +
                        "• 导入时会合并数据，不会覆盖已有记录\n" +
                        "• 若导入的记录与本地相同（同一条记录会自动去重\n" +
                        "• 换手机或重装应用后，建议先导入备份";

                new androidx.appcompat.app.AlertDialog.Builder(getContext())
                        .setTitle("数据备份说明")
                        .setMessage(message)
                        .setPositiveButton("我知道了", null)
                        .show();
            });
        }
    }

    public void setOnMonthClickListener(YearHeatmapView.OnMonthClickListener listener) {
        this.monthClickListener = listener;
    }

    public void setOnExportClickListener(OnExportClickListener listener) {
        this.exportClickListener = listener;
    }

    public void setOnImportClickListener(OnImportClickListener listener) {
        this.importClickListener = listener;
    }

    public void updateData(List<TimerRecord> records) {
        this.allRecords = records;
        
        // 计算用户最早使用年份
        if (records != null && !records.isEmpty()) {
            minYear = Integer.MAX_VALUE;
            for (TimerRecord record : records) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(record.getStart());
                int year = cal.get(Calendar.YEAR);
                if (year < minYear) {
                    minYear = year;
                }
            }
        } else {
            minYear = Calendar.getInstance().get(Calendar.YEAR);
        }
        
        maxYear = Calendar.getInstance().get(Calendar.YEAR);
        currentYear = maxYear;
        updateArrowVisibility();
        renderYearData(currentYear);
    }

    private void renderYearData(int year) {
        yearTitle.setText(year + "年 · 年度总览");
        yearHeatmap.setYearData(year, allRecords);

        int days = getYearDaysCount(year, allRecords);
        int minutes = getYearTotalMinutes(year, allRecords);
        int hours = minutes / 60;
        int remainingMinutes = minutes % 60;

        totalDays.setText("累计计时 " + days + "天");

        if (hours > 0 && remainingMinutes > 0) {
            totalHours.setText("总计 " + hours + "小时" + remainingMinutes + "分钟");
        } else if (hours > 0) {
            totalHours.setText("总计 " + hours + "小时0分钟");
        } else {
            totalHours.setText("总计 0小时" + remainingMinutes + "分钟");
        }
    }

    private void navigateYear(int direction) {
        int newYear = currentYear + direction;
        if (newYear >= minYear && newYear <= maxYear) {
            currentYear = newYear;
            updateArrowVisibility();
            renderYearData(currentYear);
        }
    }

    private void updateArrowVisibility() {
        if (yearPrev != null) {
            boolean canPrev = currentYear > minYear;
            yearPrev.setAlpha(canPrev ? 1.0f : 0.3f);
            yearPrev.setClickable(canPrev);
            yearPrev.setEnabled(canPrev);
        }
        if (yearNext != null) {
            boolean canNext = currentYear < maxYear;
            yearNext.setAlpha(canNext ? 1.0f : 0.3f);
            yearNext.setClickable(canNext);
            yearNext.setEnabled(canNext);
        }
    }
    
    private boolean hasMonthData(int year, int month) {
        if (allRecords == null || allRecords.isEmpty()) return false;
        
        Calendar cal = Calendar.getInstance();
        for (TimerRecord record : allRecords) {
            cal.setTimeInMillis(record.getStart());
            if (cal.get(Calendar.YEAR) == year && cal.get(Calendar.MONTH) == month) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isWithinUsageRange(int year, int month) {
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
        if (year > earliestYear || (year == earliestYear && month >= earliestMonth)) {
            return true;
        }
        return false;
    }

    private int getYearDaysCount(int year, List<TimerRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        long yearStart = DateUtils.getYearStartMillis(year);
        long yearEnd = DateUtils.getYearEndMillis(year);
        int count = 0;
        long lastDayStart = -1;

        for (TimerRecord record : records) {
            long start = record.getStart();
            if (start >= yearStart && start <= yearEnd) {
                long dayStart = DateUtils.getDayStartMillis(start);
                if (dayStart != lastDayStart) {
                    count++;
                    lastDayStart = dayStart;
                }
            }
        }

        return count;
    }

    private int getYearTotalMinutes(int year, List<TimerRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        long yearStart = DateUtils.getYearStartMillis(year);
        long yearEnd = DateUtils.getYearEndMillis(year);
        int total = 0;

        for (TimerRecord record : records) {
            long start = record.getStart();
            if (start >= yearStart && start <= yearEnd) {
                total += record.getDurationMin();
            }
        }

        return total;
    }

    public int getCurrentYear() {
        return currentYear;
    }
}
