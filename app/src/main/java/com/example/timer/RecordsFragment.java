package com.example.timer;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;

public class RecordsFragment extends Fragment {
    private DataManager dataManager;
    private RecyclerView dayTimelineList;
    private LinearLayout weekTimelineList;
    private TextView listHeader, chartListHeader, listHeaderCount;
    private LinearLayout listHeaderContainer;
    private View weekChartCard, monthHeatmapCard, yearHeatmapCard, chartTaskStatsCard;
    private LinearLayout viewDay, viewChart;
    private Button subTabDay, subTabWeek, subTabMonth, subTabYear;
    private DayLineChartView dayLineChart;
    private WeekBarChartView weekBarChart;
    private MonthHeatmapView monthHeatmap;
    private YearStatisticsView yearStatisticsView;
    private DayRecordsAdapter dayAdapter;
    private FloatingActionButton fabAdd;
    private TextView chartStatsTitle;
    private com.example.timer.FlowLayout chartStatsTagsLayout;
    private com.example.timer.PieChartView chartStatsPieChart;

    private RecordsDialogHelper dialogHelper;
    private RecordsChartHelper chartHelper;

    private String selectedDate;
    private static final long ANIMATION_DURATION = 300;
    private boolean isAnimating = false;

    private ActivityResultLauncher<Intent> openFileLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);

        dataManager = new DataManager(requireContext());

        dayTimelineList = view.findViewById(R.id.day_timeline_list);

        dayAdapter = new DayRecordsAdapter(
                getResources().getColor(R.color.accent),
                getResources().getColor(R.color.accent_light)
        );
        dayAdapter.setOnItemDeleteListener(position -> {
            AlertDialog dialog = new AlertDialog.Builder(requireContext())
                    .setTitle("确认删除")
                    .setMessage("确定要删除这条计时记录吗？")
                    .setPositiveButton("确认删除", (d, which) -> {
                        List<TimerRecord> adapterRecords = dayAdapter.getRecords();
                        if (position >= 0 && position < adapterRecords.size()) {
                            TimerRecord recordToDelete = adapterRecords.get(position);
                            dataManager.deleteRecord(recordToDelete);
                            dayAdapter.closeAllSwipeItems();
                            dayAdapter.removeItem(position);
                            if (dayAdapter.getItemCount() == 0) {
                                listHeaderContainer.setVisibility(View.GONE);
                            } else {
                                listHeaderCount.setText(" | " + dayAdapter.getItemCount() + "次计时");
                            }
                            updateDayChart();
                        }
                    })
                    .setNegativeButton("取消", (d, which) -> {
                        dayAdapter.closeSwipeItemAtPosition(position);
                    })
                    .create();
            dialog.show();
        });

        dayAdapter.setOnItemNameClickListener((position, record) -> {
            dialogHelper.showOperationBottomSheet(position, record);
        });
        dayTimelineList.setLayoutManager(new LinearLayoutManager(requireContext()));
        dayTimelineList.setAdapter(dayAdapter);

        weekTimelineList = view.findViewById(R.id.week_timeline_list);
        listHeader = view.findViewById(R.id.list_header);
        listHeaderCount = view.findViewById(R.id.list_header_count);
        listHeaderContainer = view.findViewById(R.id.list_header_container);
        chartListHeader = view.findViewById(R.id.chart_list_header);

        viewDay = view.findViewById(R.id.view_day);
        viewChart = view.findViewById(R.id.view_chart);
        weekChartCard = view.findViewById(R.id.week_chart_card);
        monthHeatmapCard = view.findViewById(R.id.month_heatmap_card);
        yearHeatmapCard = view.findViewById(R.id.year_heatmap_card);
        chartTaskStatsCard = view.findViewById(R.id.chart_task_stats_card);
        chartStatsTitle = view.findViewById(R.id.chart_stats_title);
        chartStatsTagsLayout = view.findViewById(R.id.chart_stats_tags_layout);
        chartStatsPieChart = view.findViewById(R.id.chart_stats_pie_chart);

        viewDay.setOnTouchListener((v, event) -> {
            dayAdapter.closeAllSwipeItems();
            return false;
        });

        fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setColorFilter(getResources().getColor(R.color.white));
        fabAdd.setOnClickListener(v -> dialogHelper.showAddRecordDialog());

        subTabDay = view.findViewById(R.id.sub_tab_day);
        subTabWeek = view.findViewById(R.id.sub_tab_week);
        subTabMonth = view.findViewById(R.id.sub_tab_month);
        subTabYear = view.findViewById(R.id.sub_tab_year);

        dayLineChart = view.findViewById(R.id.day_line_chart);
        weekBarChart = view.findViewById(R.id.week_bar_chart);
        monthHeatmap = view.findViewById(R.id.month_heatmap);
        yearStatisticsView = view.findViewById(R.id.year_statistics_view);

        if (monthHeatmap != null) {
            monthHeatmap.setOnCellClickListener((date, day, minutes) -> chartHelper.showMonthDayDetail(date, minutes));
        }

        if (yearStatisticsView != null) {
            yearStatisticsView.setOnMonthClickListener((year, month) -> {
                switchSubView("month", year, month, true);
            });
            yearStatisticsView.setOnExportClickListener(() -> startExport());
            yearStatisticsView.setOnImportClickListener(() -> startImport());
        }

        openFileLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            readJsonFromFile(uri);
                        }
                    }
                }
        );

        if (weekBarChart != null) {
            weekBarChart.setOnBarClickListener((dayData, index) -> {
                selectedDate = dayData.fullDate;
                switchSubView("day", -1, -1, true);
            });
        }

        subTabDay.setOnClickListener(v -> {
            selectedDate = null;
            switchSubView("day");
        });
        subTabWeek.setOnClickListener(v -> switchSubView("week"));
        subTabMonth.setOnClickListener(v -> switchSubView("month"));
        subTabYear.setOnClickListener(v -> switchSubView("year"));

        dialogHelper = new RecordsDialogHelper(requireContext(), dataManager, new RecordsDialogHelper.OnRecordChangedListener() {
            @Override
            public void onRecordAdded() {
                updateDayView();
            }

            @Override
            public void onRecordUpdated(int position, TimerRecord record) {
                updateRecordAndRefresh(position, record);
            }
        });

        chartHelper = new RecordsChartHelper(requireContext(), dataManager,
                date -> {
                    selectedDate = date;
                    switchSubView("day", -1, -1, true);
                },
                chartListHeader, weekTimelineList, chartStatsTagsLayout, chartStatsTitle,
                chartTaskStatsCard, chartStatsPieChart);

        switchSubView("day");

        return view;
    }

    public void refresh() {
        String currentView = getCurrentView();
        switchSubView(currentView);
    }

    private String getCurrentView() {
        if (viewDay.getVisibility() == View.VISIBLE) return "day";
        if (viewChart.getVisibility() == View.VISIBLE) {
            if (subTabWeek.isSelected()) return "week";
            if (subTabMonth.isSelected()) return "month";
            if (subTabYear.isSelected()) return "year";
        }
        return "day";
    }

    private void updateSubTabState(String view) {
        subTabDay.setBackgroundResource(view.equals("day") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabDay.setTextColor(view.equals("day") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabDay.setSelected(view.equals("day"));

        subTabWeek.setBackgroundResource(view.equals("week") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabWeek.setTextColor(view.equals("week") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabWeek.setSelected(view.equals("week"));

        subTabMonth.setBackgroundResource(view.equals("month") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabMonth.setTextColor(view.equals("month") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabMonth.setSelected(view.equals("month"));

        subTabYear.setBackgroundResource(view.equals("year") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabYear.setTextColor(view.equals("year") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabYear.setSelected(view.equals("year"));
    }

    private void switchSubView(String view) {
        switchSubView(view, -1, -1, false);
    }

    private void switchSubView(String view, int year, int month) {
        switchSubView(view, year, month, false);
    }

    private void switchSubView(String view, int year, int month, boolean withAnimation) {
        if (isAnimating) {
            return;
        }

        if (dayAdapter != null) {
            dayAdapter.closeAllSwipeItems();
        }

        if (monthHeatmap != null) {
            monthHeatmap.clearSelectedState();
        }

        if (dayLineChart != null) {
            dayLineChart.clearTouchState();
        }

        if (weekBarChart != null) {
            weekBarChart.clearTouchState();
        }

        subTabDay.setBackgroundResource(view.equals("day") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabDay.setTextColor(view.equals("day") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabDay.setSelected(view.equals("day"));

        subTabWeek.setBackgroundResource(view.equals("week") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabWeek.setTextColor(view.equals("week") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabWeek.setSelected(view.equals("week"));

        subTabMonth.setBackgroundResource(view.equals("month") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabMonth.setTextColor(view.equals("month") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabMonth.setSelected(view.equals("month"));

        subTabYear.setBackgroundResource(view.equals("year") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabYear.setTextColor(view.equals("year") ? getResources().getColor(R.color.white) : getResources().getColor(R.color.accent));
        subTabYear.setSelected(view.equals("year"));

        // 如果切换到日视图，且不带动画且已经在日视图，直接更新数据
        if (view.equals("day") && !withAnimation && viewDay.getVisibility() == View.VISIBLE) {
            updateDayView();
            return;
        }

        View currentView = viewDay.getVisibility() == View.VISIBLE ? viewDay : viewChart;
        
        if (withAnimation) {
            isAnimating = true;
            currentView.animate()
                    .alpha(0f)
                    .translationX(-50)
                    .setDuration(ANIMATION_DURATION)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            currentView.setVisibility(View.GONE);
                            currentView.setAlpha(1f);
                            currentView.setTranslationX(0);
                            
                            showNewViewWithAnimation(view, year, month);
                        }
                    });
        } else {
            currentView.setVisibility(View.GONE);
            showNewViewWithoutAnimation(view, year, month);
        }
    }

    private void showNewViewWithAnimation(String view, int year, int month) {
        View newView;
        
        if (view.equals("day")) {
            newView = viewDay;
            fabAdd.setVisibility(View.VISIBLE);
            updateDayView();
        } else {
            fabAdd.setVisibility(View.GONE);
            newView = viewChart;
            
            if (view.equals("week")) {
                weekChartCard.setVisibility(View.VISIBLE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.GONE);
                renderWeekChart();
                renderWeekList();
            } else if (view.equals("month")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.VISIBLE);
                yearHeatmapCard.setVisibility(View.GONE);
                if (year > 0 && month >= 0) {
                    renderMonthHeatmap(year, month);
                } else {
                    renderMonthHeatmap();
                }
                chartHelper.clearMonthDayList();
            } else if (view.equals("year")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.VISIBLE);
                chartTaskStatsCard.setVisibility(View.GONE);
                renderYearHeatmap();
                chartHelper.clearMonthDayList();
            }
        }

        newView.setAlpha(0f);
        newView.setTranslationX(50);
        newView.setVisibility(View.VISIBLE);

        newView.animate()
                .alpha(1f)
                .translationX(0)
                .setDuration(ANIMATION_DURATION)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        isAnimating = false;
                    }
                });
    }

    private void showNewViewWithoutAnimation(String view, int year, int month) {
        View newView;
        
        if (view.equals("day")) {
            newView = viewDay;
            fabAdd.setVisibility(View.VISIBLE);
            updateDayView();
        } else {
            fabAdd.setVisibility(View.GONE);
            newView = viewChart;
            
            if (view.equals("week")) {
                weekChartCard.setVisibility(View.VISIBLE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.GONE);
                renderWeekChart();
                renderWeekList();
            } else if (view.equals("month")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.VISIBLE);
                yearHeatmapCard.setVisibility(View.GONE);
                if (year > 0 && month >= 0) {
                    renderMonthHeatmap(year, month);
                } else {
                    renderMonthHeatmap();
                }
                chartHelper.clearMonthDayList();
            } else if (view.equals("year")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.VISIBLE);
                chartTaskStatsCard.setVisibility(View.GONE);
                renderYearHeatmap();
                chartHelper.clearMonthDayList();
            }
        }

        newView.setAlpha(1f);
        newView.setTranslationX(0);
        newView.setVisibility(View.VISIBLE);
    }

    private void updateDayView() {
        List<TimerRecord> dayRecords = RecordsDataFilter.filterByDate(dataManager.getRecords(), selectedDate);

        if (dayLineChart != null) {
            // 设置折线图标题
            String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            long selectedMillis;
            String dateDisplay;
            if (selectedDate == null) {
                // 今天
                selectedMillis = System.currentTimeMillis();
                dateDisplay = DateUtils.formatDate(selectedMillis);
            } else {
                String[] parts = selectedDate.split("-");
                int year = Integer.parseInt(parts[0]);
                int month = Integer.parseInt(parts[1]) - 1;
                int day = Integer.parseInt(parts[2]);
                selectedMillis = DateUtils.getSpecificDayStartMillis(year, month, day);
                dateDisplay = selectedDate;
            }
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedMillis);
            String dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1];
            dayLineChart.setTitlePrefix(dateDisplay + "（" + dayOfWeek + "）");
            dayLineChart.setData(dayRecords);
        }

        renderDayList(dayRecords);
    }

    private void updateDayChart() {
        if (dayLineChart != null) {
            dayLineChart.setData(RecordsDataFilter.filterByDate(dataManager.getRecords(), selectedDate));
        }
    }

    private void renderWeekChart() {
        if (weekBarChart == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<WeekBarChartView.DayData> dayDataList = new ArrayList<>();
        List<TimerRecord> weekRecords = new ArrayList<>();

        List<TimerRecord> allRecords = dataManager.getRecords();

        for (int i = 0; i < 7; i++) {
            long dayStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());
            long dayEnd = dayStart + 24L * 60 * 60 * 1000;
            String dateStr = DateUtils.formatDate(dayStart);
            int dayMinutes = 0;
            String taskType = "";

            for (TimerRecord record : allRecords) {
                long start = record.getStart();
                if (start >= dayStart && start < dayEnd) {
                    dayMinutes += record.getDurationMin();
                    weekRecords.add(record);
                    if (taskType.isEmpty() && record.getName() != null) {
                        taskType = record.getName();
                    }
                }
            }

            String dateLabel = DateUtils.formatShortDate(dayStart);
            dayDataList.add(new WeekBarChartView.DayData(weekDays[i], dateLabel, dateStr, dayMinutes, taskType));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        weekBarChart.setData(dayDataList);
        chartHelper.renderChartTaskStats(weekRecords, "本周统计");
    }

    private void renderMonthHeatmap() {
        if (monthHeatmap == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        renderMonthHeatmap(year, month);
    }

    private void renderMonthHeatmap(int year, int month) {
        if (monthHeatmap == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(year, month, 1);
        int displayYear = cal.get(Calendar.YEAR);
        int displayMonth = cal.get(Calendar.MONTH) + 1;
        int firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1;

        int[] dailyMinutes = new int[31];
        List<TimerRecord> monthRecords = new ArrayList<>();
        List<TimerRecord> allRecords = dataManager.getRecords();
        long monthStart = DateUtils.getMonthStartMillis(displayYear, displayMonth - 1);
        long monthEnd = DateUtils.getMonthEndMillis(displayYear, displayMonth - 1);

        for (TimerRecord record : allRecords) {
            long start = record.getStart();
            if (start >= monthStart && start <= monthEnd) {
                int dayIndex = DateUtils.getDay(start) - 1;
                if (dayIndex >= 0 && dayIndex < 31) {
                    dailyMinutes[dayIndex] += record.getDurationMin();
                }
                monthRecords.add(record);
            }
        }

        monthHeatmap.setMonthData(dailyMinutes, firstDayOfWeek, displayYear, displayMonth);
        chartHelper.renderChartTaskStats(monthRecords, "本月统计");
    }

    private void renderYearHeatmap() {
        if (yearStatisticsView != null) {
            yearStatisticsView.updateData(dataManager.getRecords());
        }
    }

    private void startExport() {
        String timeStamp = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "timer-backup-" + timeStamp + ".json";
        String json = dataManager.exportRecordsToJson();
        int total = dataManager.getRecordCount();

        try {
            android.content.ContentResolver resolver = requireContext().getContentResolver();
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json");
            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/TimerBackup");

            Uri fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (fileUri == null) {
                Toast.makeText(requireContext(), "导出失败：无法创建文件", Toast.LENGTH_LONG).show();
                return;
            }

            OutputStream outputStream = null;
            OutputStreamWriter writer = null;
            try {
                outputStream = resolver.openOutputStream(fileUri);
                if (outputStream == null) {
                    Toast.makeText(requireContext(), "导出失败：无法写入文件", Toast.LENGTH_LONG).show();
                    return;
                }
                writer = new OutputStreamWriter(outputStream);
                writer.write(json);
                writer.flush();
                new AlertDialog.Builder(requireContext())
                        .setTitle("导出成功")
                        .setMessage("已成功导出 " + total + " 条计时记录。\n\n备份文件已保存到：\n\n📁 内部存储/下载/TimerBackup/\n📄 " + fileName + "\n\n如需恢复数据，在导入数据时选择该文件即可。")
                        .setPositiveButton("我知道了", null)
                        .show();
            } catch (IOException e) {
                Toast.makeText(requireContext(), "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                if (writer != null) { try { writer.close(); } catch (IOException ignored) {} }
                if (outputStream != null) { try { outputStream.close(); } catch (IOException ignored) {} }
            }
        } catch (Exception e) {
            Toast.makeText(requireContext(), "导出失败：" + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void startImport() {
        AlertDialog confirm = new AlertDialog.Builder(requireContext())
                .setTitle("导入数据")
                .setMessage("将从备份文件中读取记录并与现有数据合并（已存在的记录不会重复导入）。\n\n请在文件管理器中找到：\n下载/TimerBackup/ 目录\n下的 .json 备份文件。")
                .setPositiveButton("继续导入", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("application/json");
                    try {
                        openFileLauncher.launch(intent);
                    } catch (Exception e) {
                        Toast.makeText(requireContext(), "导入失败：无法打开文件选择器", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("取消", null)
                .create();
        confirm.show();
    }

    private void readJsonFromFile(Uri uri) {
        StringBuilder jsonBuilder = new StringBuilder();
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            inputStream = requireContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                Toast.makeText(requireContext(), "导入失败：无法读取文件", Toast.LENGTH_SHORT).show();
                return;
            }
            reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null) {
                jsonBuilder.append(line);
            }
        } catch (IOException e) {
            Toast.makeText(requireContext(), "导入失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        } finally {
            if (reader != null) {
                try { reader.close(); } catch (IOException ignored) {}
            }
            if (inputStream != null) {
                try { inputStream.close(); } catch (IOException ignored) {}
            }
        }
        int added = dataManager.importRecordsFromJson(jsonBuilder.toString());
        if (added > 0) {
            Toast.makeText(requireContext(), "已导入 " + added + " 条新记录", Toast.LENGTH_LONG).show();
            refreshAllData();
        } else {
            Toast.makeText(requireContext(), "未找到可导入的新记录", Toast.LENGTH_LONG).show();
        }
    }

    private void refreshAllData() {
        List<TimerRecord> records = dataManager.getRecords();
        if (dayAdapter != null) {
            dayAdapter.setRecords(new ArrayList<>());
        }
        List<TimerRecord> dayRecords = RecordsDataFilter.filterByDate(records, selectedDate);
        renderDayList(dayRecords);
        updateDayChart();
        renderWeekChart();
        if (monthHeatmap != null) {
            renderMonthHeatmap();
        }
        if (yearStatisticsView != null) {
            yearStatisticsView.updateData(records);
        }
    }

    private void renderDayList(List<TimerRecord> records) {
        // 不管记录是否为空，都设置列表数据
        if (selectedDate == null) {
            listHeader.setText("今日记录");
        } else {
            String[] parts = selectedDate.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);
            long selectedMillis = DateUtils.getSpecificDayStartMillis(year, month, day);
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(selectedMillis);

            String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            String dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1];

            listHeader.setText(selectedDate + "（" + dayOfWeek + "）");
        }

        listHeaderCount.setText(" | " + records.size() + "次计时");

        if (records.isEmpty()) {
            listHeaderContainer.setVisibility(View.GONE);
        } else {
            listHeaderContainer.setVisibility(View.VISIBLE);
            listHeaderCount.setVisibility(View.VISIBLE);
        }

        // 强制更新适配器数据
        dayAdapter.setRecords(new ArrayList<>(records));
        // 确保 RecyclerView 刷新
        dayTimelineList.post(() -> {
            dayTimelineList.invalidate();
            dayTimelineList.requestLayout();
        });
    }

    private void renderWeekList() {
        if (weekTimelineList == null) return;
        weekTimelineList.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        int[] dayMinutes = new int[7];
        String[] dayDates = new String[7];
        String[] dayDateStrings = new String[7];

        List<TimerRecord> allRecords = dataManager.getRecords();

        for (int i = 0; i < 7; i++) {
            long dayStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());
            long dayEnd = dayStart + 24L * 60 * 60 * 1000;
            String dateStr = DateUtils.formatDate(dayStart);
            dayDateStrings[i] = dateStr;
            dayDates[i] = DateUtils.formatShortDate(dayStart);
            for (TimerRecord record : allRecords) {
                long start = record.getStart();
                if (start >= dayStart && start < dayEnd) {
                    dayMinutes[i] += record.getDurationMin();
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        listHeader.setVisibility(View.VISIBLE);
        listHeader.setText("本周记录");
        listHeaderCount.setVisibility(View.GONE);
        listHeaderContainer.setVisibility(View.VISIBLE);
        if (chartListHeader != null) {
            chartListHeader.setVisibility(View.VISIBLE);
            chartListHeader.setText("本周记录");
        }

        for (int i = 6; i >= 0; i--) {
            if (dayMinutes[i] == 0) continue;

            final int dayIndex = i;
            final String dateStr = dayDateStrings[i];

            View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.timeline_item, null);
            TextView name = itemView.findViewById(R.id.record_name);
            TextView timeRange = itemView.findViewById(R.id.record_time);
            TextView duration = itemView.findViewById(R.id.record_duration);

            name.setText(weekDays[i]);
            timeRange.setText(dayDates[i]);

            int hours = dayMinutes[i] / 60;
            int mins = dayMinutes[i] % 60;
            if (hours > 0) {
                duration.setText(hours + "小时" + mins + "分钟");
            } else {
                duration.setText(mins + "分钟");
            }

            itemView.findViewById(R.id.timeline_dot).setBackgroundColor(getResources().getColor(R.color.accent));
            itemView.findViewById(R.id.delete_btn).setVisibility(View.GONE);

            View clickableContent = itemView.findViewById(R.id.clickable_content);
            clickableContent.setOnClickListener(v -> chartHelper.showDayPieChartDialog(dateStr, weekDays[dayIndex], dayDates[dayIndex], false));

            weekTimelineList.addView(itemView);
        }

        if (weekTimelineList.getChildCount() == 0) {
            if (chartListHeader != null) {
                chartListHeader.setVisibility(View.GONE);
            }
        }
    }

    private void updateRecordAndRefresh(int position, TimerRecord record) {
        dataManager.updateRecord(record);

        List<TimerRecord> records = dayAdapter.getRecords();
        if (position >= 0 && position < records.size()) {
            records.set(position, record);
            dayAdapter.notifyItemChanged(position);
        }

        updateDayView();
    }
}