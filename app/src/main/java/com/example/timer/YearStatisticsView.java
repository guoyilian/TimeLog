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
    private YearHeatmapView.OnMonthClickListener monthClickListener;

    private int currentYear;
    private List<TimerRecord> allRecords;
    private List<Integer> availableYears;
    private boolean isInitializing = true;
    private boolean hasInitializedYear = false; // 标记是否已经初始化过年份

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
    }

    public void setOnMonthClickListener(YearHeatmapView.OnMonthClickListener listener) {
        this.monthClickListener = listener;
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
            if (record.getStart() != null && record.getStart().length() >= 4) {
                try {
                    int year = Integer.parseInt(record.getStart().substring(0, 4));
                    if (!years.contains(year)) {
                        years.add(year);
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
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

        String yearStr = String.format("%04d", year);
        int count = 0;
        String lastDate = "";

        for (TimerRecord record : records) {
            if (record.getStart() != null && record.getStart().startsWith(yearStr)) {
                String date = record.getStart().substring(0, 10);
                if (!date.equals(lastDate)) {
                    count++;
                    lastDate = date;
                }
            }
        }

        return count;
    }

    private int getYearTotalMinutes(int year, List<TimerRecord> records) {
        if (records == null || records.isEmpty()) {
            return 0;
        }

        String yearStr = String.format("%04d", year);
        int total = 0;

        for (TimerRecord record : records) {
            if (record.getStart() != null && record.getStart().startsWith(yearStr)) {
                total += record.getDurationMin();
            }
        }

        return total;
    }

    public int getCurrentYear() {
        return currentYear;
    }
}
