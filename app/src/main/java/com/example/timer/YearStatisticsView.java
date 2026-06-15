package com.example.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class YearStatisticsView extends LinearLayout {
    private YearHeatmapView yearHeatmap;
    private TextView yearTitle;
    private TextView totalDays;
    private TextView totalHours;
    private Spinner yearSpinner;
    private View btnExport;
    private View btnImport;
    private YearHeatmapView.OnMonthClickListener monthClickListener;
    private OnExportClickListener exportClickListener;
    private OnImportClickListener importClickListener;

    private int currentYear;
    private List<TimerRecord> allRecords;
    private List<Integer> availableYears;
    private boolean isInitializing = true;
    private boolean hasInitializedYear = false; // 标记是否已经初始化过年份

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
        totalDays = findViewById(R.id.total_days);
        totalHours = findViewById(R.id.total_hours);
        yearSpinner = findViewById(R.id.year_spinner);

        Calendar calendar = Calendar.getInstance();
        currentYear = calendar.get(Calendar.YEAR);
        yearTitle.setText(currentYear + "年 · 年度总览");

        yearHeatmap.setOnMonthClickListener((year, month) -> {
            if (monthClickListener != null) {
                monthClickListener.onMonthClick(year, month);
            }
        });

        yearSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!isInitializing && availableYears != null && position < availableYears.size()) {
                    int selectedYear = availableYears.get(position);
                    if (selectedYear != currentYear) {
                        currentYear = selectedYear;
                        renderYearData(currentYear);
                    }
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
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
        // 只有首次初始化时才设置为今年，之后保持用户选择的年份
        if (!hasInitializedYear) {
            currentYear = Calendar.getInstance().get(Calendar.YEAR);
            hasInitializedYear = true;
        }
        populateYearSpinner();
        renderYearData(currentYear);
    }

    public void updateDataForYear(List<TimerRecord> records, int year) {
        this.allRecords = records;
        currentYear = year;
        populateYearSpinner();
        renderYearData(year);
    }

    private void populateYearSpinner() {
        availableYears = extractYearsFromRecords(allRecords);
        int currentYearNow = Calendar.getInstance().get(Calendar.YEAR);
        if (!availableYears.contains(currentYearNow)) {
            availableYears.add(currentYearNow);
        }
        java.util.Collections.sort(availableYears, (a, b) -> b - a);

        List<String> yearStrings = new ArrayList<>();
        for (Integer year : availableYears) {
            yearStrings.add(year + "年");
        }

        Context context = getContext();
        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, 
                android.R.layout.simple_spinner_item, yearStrings);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        
        isInitializing = true;
        yearSpinner.setAdapter(adapter);
        
        int currentYearIndex = availableYears.indexOf(currentYear);
        if (currentYearIndex >= 0) {
            yearSpinner.setSelection(currentYearIndex);
        }
        isInitializing = false;
    }

    private List<Integer> extractYearsFromRecords(List<TimerRecord> records) {
        List<Integer> years = new ArrayList<>();

        if (records == null || records.isEmpty()) {
            return years;
        }

        for (TimerRecord record : records) {
            int year = DateUtils.getYear(record.getStart());
            if (!years.contains(year)) {
                years.add(year);
            }
        }

        return years;
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
