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
    private View weekChartCard, monthHeatmapCard, yearHeatmapCard, chartTaskStatsCard, dayTaskStatsCard;
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
    private TextView dayStatsTitle;
    private com.example.timer.PieChartView dayStatsPieChart;
    private LinearLayout dayStatsTagsLayout;

    private RecordsDialogHelper dialogHelper;
    private RecordsChartHelper chartHelper;

    private String selectedDate;
    private static final long ANIMATION_DURATION = 300;
    private boolean isAnimating = false;

    // ===== 时间导航状态 =====
    private long currentDayMillis;        // 日视图：当前查看的那一天（00:00毫秒）
    private long currentWeekStartMillis;  // 周视图：当前查看周的周一 00:00 毫秒
    private int currentMonthYear;         // 月视图：当前查看的年份
    private int currentMonth;             // 月视图：当前查看的月份（0=一月...11=十二月）
    private long firstDataDayMillis;      // 所有记录中的最早日期（00:00毫秒）
    private long lastDataDayMillis;       // 所有记录中的最晚日期（00:00毫秒）

    private TextView dayPrev, dayTitle, dayNext;
    private TextView weekPrev, weekTitle, weekNext;
    private TextView monthPrev, monthTitle, monthNext;


    private ActivityResultLauncher<Intent> openFileLauncher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_records, container, false);

        dataManager = new DataManager(requireContext());

        // ===== 初始化时间状态：默认指向今天/本周/本月 =====
        long now = System.currentTimeMillis();
        currentDayMillis = DateUtils.getDayStartMillis(now);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStartMillis = DateUtils.getDayStartMillis(cal.getTimeInMillis());

        Calendar calMonth = Calendar.getInstance();
        calMonth.setTimeInMillis(now);
        currentMonthYear = calMonth.get(Calendar.YEAR);
        currentMonth = calMonth.get(Calendar.MONTH);

        // 初始化数据范围
        updateDataRange();

        dayTimelineList = view.findViewById(R.id.day_timeline_list);

        dayAdapter = new DayRecordsAdapter(
                getResources().getColor(R.color.accent, null),
                getResources().getColor(R.color.accent_light, null)
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
                                listHeaderCount.setText(getString(R.string.record_count_format, dayAdapter.getItemCount()));
                            }
                            updateDayView();
                        }
                    })
                    .setNegativeButton("取消", (d, which) -> dayAdapter.closeSwipeItemAtPosition(position))
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

        dayTaskStatsCard = view.findViewById(R.id.day_task_stats_card);
        dayStatsTitle = view.findViewById(R.id.day_stats_title);
        dayStatsPieChart = view.findViewById(R.id.day_stats_pie_chart);
        dayStatsTagsLayout = view.findViewById(R.id.day_stats_tags_layout);

        viewDay.setOnTouchListener((v, event) -> {
            dayAdapter.closeAllSwipeItems();
            return false;
        });

        fabAdd = view.findViewById(R.id.fab_add);
        fabAdd.setColorFilter(getResources().getColor(R.color.white, null));
        fabAdd.setOnClickListener(v -> dialogHelper.showAddRecordDialog());

        subTabDay = view.findViewById(R.id.sub_tab_day);
        subTabWeek = view.findViewById(R.id.sub_tab_week);
        subTabMonth = view.findViewById(R.id.sub_tab_month);
        subTabYear = view.findViewById(R.id.sub_tab_year);

        dayLineChart = view.findViewById(R.id.day_line_chart);
        weekBarChart = view.findViewById(R.id.week_bar_chart);
        monthHeatmap = view.findViewById(R.id.month_heatmap);
        yearStatisticsView = view.findViewById(R.id.year_statistics_view);

        // ===== 导航栏视图引用 =====
        dayPrev = view.findViewById(R.id.day_prev);
        dayTitle = view.findViewById(R.id.day_title);
        dayNext = view.findViewById(R.id.day_next);

        weekPrev = view.findViewById(R.id.week_prev);
        weekTitle = view.findViewById(R.id.week_title);
        weekNext = view.findViewById(R.id.week_next);

        monthPrev = view.findViewById(R.id.month_prev);
        monthTitle = view.findViewById(R.id.month_title);
        monthNext = view.findViewById(R.id.month_next);


        // ===== 导航栏点击事件 =====
        if (dayPrev != null) {
            dayPrev.setOnClickListener(v -> navigatePrevDay());
        }
        if (dayNext != null) {
            dayNext.setOnClickListener(v -> navigateNextDay());
        }

        if (weekPrev != null) {
            weekPrev.setOnClickListener(v -> navigatePrevWeek());
        }
        if (weekNext != null) {
            weekNext.setOnClickListener(v -> navigateNextWeek());
        }

        if (monthPrev != null) {
            monthPrev.setOnClickListener(v -> navigatePrevMonth());
        }
        if (monthNext != null) {
            monthNext.setOnClickListener(v -> navigateNextMonth());
        }

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
        
        // 注册数据变化监听
        dataManager.addOnDataChangedListener(this::updateDataRange);

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
                chartTaskStatsCard, chartStatsPieChart,
                dayTaskStatsCard, dayStatsTitle, dayStatsPieChart, dayStatsTagsLayout);

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
        subTabDay.setTextColor(view.equals("day") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
        subTabDay.setSelected(view.equals("day"));

        subTabWeek.setBackgroundResource(view.equals("week") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabWeek.setTextColor(view.equals("week") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
        subTabWeek.setSelected(view.equals("week"));

        subTabMonth.setBackgroundResource(view.equals("month") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabMonth.setTextColor(view.equals("month") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
        subTabMonth.setSelected(view.equals("month"));

        subTabYear.setBackgroundResource(view.equals("year") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabYear.setTextColor(view.equals("year") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
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
        subTabDay.setTextColor(view.equals("day") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
        subTabDay.setSelected(view.equals("day"));

        subTabWeek.setBackgroundResource(view.equals("week") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabWeek.setTextColor(view.equals("week") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
        subTabWeek.setSelected(view.equals("week"));

        subTabMonth.setBackgroundResource(view.equals("month") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabMonth.setTextColor(view.equals("month") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
        subTabMonth.setSelected(view.equals("month"));

        subTabYear.setBackgroundResource(view.equals("year") ? R.drawable.subtab_active_bg : R.drawable.subtab_inactive_bg);
        subTabYear.setTextColor(view.equals("year") ? getResources().getColor(R.color.white, null) : getResources().getColor(R.color.accent, null));
        subTabYear.setSelected(view.equals("year"));

        // 如果切换到日视图，且不带动画且已经在日视图，重置为今天再更新
        if (view.equals("day") && !withAnimation && viewDay.getVisibility() == View.VISIBLE) {
            currentDayMillis = DateUtils.getDayStartMillis(System.currentTimeMillis());
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
            if (selectedDate != null && !selectedDate.isEmpty()) {
                long parsedMillis = DateUtils.parseDate(selectedDate);
                if (parsedMillis > 0) {
                    currentDayMillis = parsedMillis;
                } else {
                    currentDayMillis = DateUtils.getDayStartMillis(System.currentTimeMillis());
                }
                selectedDate = null;
            } else {
                currentDayMillis = DateUtils.getDayStartMillis(System.currentTimeMillis());
            }
            updateDayView();
        } else {
            fabAdd.setVisibility(View.GONE);
            newView = viewChart;
            
            if (view.equals("week")) {
                weekChartCard.setVisibility(View.VISIBLE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.GONE);
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                currentWeekStartMillis = DateUtils.getDayStartMillis(cal.getTimeInMillis());
                renderWeekChart();
                renderWeekList();
                updateWeekTitle();
            } else if (view.equals("month")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.VISIBLE);
                yearHeatmapCard.setVisibility(View.GONE);
                if (year > 0 && month >= 0) {
                    currentMonthYear = year;
                    currentMonth = month;
                    renderMonthHeatmap(year, month);
                } else {
                    Calendar calMonth = Calendar.getInstance();
                    currentMonthYear = calMonth.get(Calendar.YEAR);
                    currentMonth = calMonth.get(Calendar.MONTH);
                    renderMonthHeatmap();
                }
                updateMonthTitle();
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
            if (selectedDate != null && !selectedDate.isEmpty()) {
                long parsedMillis = DateUtils.parseDate(selectedDate);
                if (parsedMillis > 0) {
                    currentDayMillis = parsedMillis;
                } else {
                    currentDayMillis = DateUtils.getDayStartMillis(System.currentTimeMillis());
                }
                selectedDate = null;
            } else {
                currentDayMillis = DateUtils.getDayStartMillis(System.currentTimeMillis());
            }
            updateDayView();
        } else {
            fabAdd.setVisibility(View.GONE);
            newView = viewChart;
            
            if (view.equals("week")) {
                weekChartCard.setVisibility(View.VISIBLE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.GONE);
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                currentWeekStartMillis = DateUtils.getDayStartMillis(cal.getTimeInMillis());
                renderWeekChart();
                renderWeekList();
                updateWeekTitle();
            } else if (view.equals("month")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.VISIBLE);
                yearHeatmapCard.setVisibility(View.GONE);
                if (year > 0 && month >= 0) {
                    currentMonthYear = year;
                    currentMonth = month;
                    renderMonthHeatmap(year, month);
                } else {
                    Calendar calMonth = Calendar.getInstance();
                    currentMonthYear = calMonth.get(Calendar.YEAR);
                    currentMonth = calMonth.get(Calendar.MONTH);
                    renderMonthHeatmap();
                }
                updateMonthTitle();
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

    private void updateDataRange() {
        List<TimerRecord> allRecords = dataManager.getRecords();
        long todayMillis = DateUtils.getDayStartMillis(System.currentTimeMillis());
        
        if (allRecords != null && !allRecords.isEmpty()) {
            long earliestMillis = Long.MAX_VALUE;
            long latestMillis = Long.MIN_VALUE;
            
            for (TimerRecord record : allRecords) {
                long recordStart = record.getStart();
                if (recordStart < earliestMillis) {
                    earliestMillis = recordStart;
                }
                if (recordStart > latestMillis) {
                    latestMillis = recordStart;
                }
            }
            
            firstDataDayMillis = DateUtils.getDayStartMillis(earliestMillis);
            lastDataDayMillis = Math.max(DateUtils.getDayStartMillis(latestMillis), todayMillis);
        } else {
            // 如果没有记录，默认数据范围为今天
            firstDataDayMillis = todayMillis;
            lastDataDayMillis = todayMillis;
        }
        
        // 确保当前查看的视图在有效范围内
        adjustCurrentViewToValidRange();
        
        // 更新所有视图的箭头状态
        updateAllArrowStatus();
    }
    
    private void adjustCurrentViewToValidRange() {
        // 日视图调整
        if (currentDayMillis < firstDataDayMillis) {
            currentDayMillis = firstDataDayMillis;
            updateDayView();
        } else if (currentDayMillis > lastDataDayMillis) {
            currentDayMillis = lastDataDayMillis;
            updateDayView();
        }
        
        // 周视图调整
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        
        // 计算最早数据所在周
        cal.setTimeInMillis(firstDataDayMillis);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long firstDataWeekStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());
        
        // 计算最晚数据所在周
        cal.setTimeInMillis(lastDataDayMillis);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long lastDataWeekStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());
        
        if (currentWeekStartMillis < firstDataWeekStart) {
            currentWeekStartMillis = firstDataWeekStart;
            updateWeekView();
        } else if (currentWeekStartMillis > lastDataWeekStart) {
            currentWeekStartMillis = lastDataWeekStart;
            updateWeekView();
        }
        
        // 月视图调整
        cal.setTimeInMillis(firstDataDayMillis);
        int firstDataYear = cal.get(Calendar.YEAR);
        int firstDataMonth = cal.get(Calendar.MONTH);
        
        cal.setTimeInMillis(lastDataDayMillis);
        int lastDataYear = cal.get(Calendar.YEAR);
        int lastDataMonth = cal.get(Calendar.MONTH);
        
        if (currentMonthYear < firstDataYear || (currentMonthYear == firstDataYear && currentMonth < firstDataMonth)) {
            currentMonthYear = firstDataYear;
            currentMonth = firstDataMonth;
            updateMonthView();
        } else if (currentMonthYear > lastDataYear || (currentMonthYear == lastDataYear && currentMonth > lastDataMonth)) {
            currentMonthYear = lastDataYear;
            currentMonth = lastDataMonth;
            updateMonthView();
        }
    }
    
    private void updateAllArrowStatus() {
        updateDayTitle();
        updateWeekTitle();
        updateMonthTitle();
    }

    private void updateDayView() {
        // 根据 currentDayMillis 筛选当天记录
        String dateStr = DateUtils.formatDate(currentDayMillis);
        List<TimerRecord> dayRecords = RecordsDataFilter.filterByDate(dataManager.getRecords(), dateStr);

        if (dayLineChart != null) {
            String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
            Calendar cal = Calendar.getInstance();
            cal.setTimeInMillis(currentDayMillis);
            String dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1];
            dayLineChart.setTitlePrefix(dateStr + "（" + dayOfWeek + "）");
            dayLineChart.setData(dayRecords);
        }

        renderDayList(dayRecords);
        updateDayTitle();

        boolean isToday = DateUtils.isSameDay(currentDayMillis, System.currentTimeMillis());
        chartHelper.renderDayTaskStats(dayRecords, isToday);
    }

    private void updateDayChart() {
        if (dayLineChart != null) {
            dayLineChart.setData(RecordsDataFilter.filterByDate(dataManager.getRecords(), selectedDate));
        }
    }

    private void updateWeekView() {
        renderWeekChart();
        renderWeekList();
        updateWeekTitle();
    }

    private void updateMonthView() {
        renderMonthHeatmap();
        updateMonthTitle();
        // 月视图列表渲染已整合在renderMonthHeatmap中
    }

    private void renderWeekChart() {
    renderWeekChart(currentWeekStartMillis);
}

    private void renderWeekChart(long weekStartMillis) {
        if (weekBarChart == null) return;

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(weekStartMillis);

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
        renderMonthHeatmap(currentMonthYear, currentMonth);
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
        String json = dataManager.exportRecordsToJson();
        int total = dataManager.getRecordCount();
        if (total == 0) {
            Toast.makeText(requireContext(), "没有数据可以导出", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = String.format("TimerBackup_%s.json",
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()));

        try {
            android.content.ContentResolver resolver = requireContext().getContentResolver();
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName);
            values.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/json");
            values.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/TimerBackup");

            Uri fileUri;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                fileUri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            } else {
                // 兼容Android Q以下版本
                java.io.File downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS);
                java.io.File timerDir = new java.io.File(downloadsDir, "TimerBackup");
                if (!timerDir.exists() && !timerDir.mkdirs()) {
                    Toast.makeText(requireContext(), "导出失败：无法创建目录", Toast.LENGTH_LONG).show();
                    return;
                }
                java.io.File file = new java.io.File(timerDir, fileName);
                fileUri = android.net.Uri.fromFile(file);
                // 使用传统方式写入文件
                java.io.FileOutputStream outputStream = new java.io.FileOutputStream(file);
                java.io.OutputStreamWriter writer = new java.io.OutputStreamWriter(outputStream);
                writer.write(json);
                writer.flush();
                writer.close();
                outputStream.close();
                
                new AlertDialog.Builder(requireContext())
                        .setTitle("导出成功")
                        .setMessage("已成功导出 " + total + " 条计时记录。\n\n备份文件已保存到：\n\n📁 内部存储/下载/TimerBackup/\n📄 " + fileName + "\n\n如需恢复数据，在导入数据时选择该文件即可。")
                        .setPositiveButton("我知道了", null)
                        .show();
                return;
            }
            
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
        updateDayView();
        renderWeekChart();
        if (monthHeatmap != null) {
            renderMonthHeatmap();
        }
        if (yearStatisticsView != null) {
            yearStatisticsView.updateData(records);
        }
    }

    private void renderDayList(List<TimerRecord> records) {
        // 使用 currentDayMillis 显示日期标题
        String dateStr = DateUtils.formatDate(currentDayMillis);
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentDayMillis);
        String dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1];
        listHeader.setText(dateStr + "（" + dayOfWeek + "）");

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
        renderWeekList(currentWeekStartMillis);
    }

    private void renderWeekList(long weekStartMillis) {
        if (weekTimelineList == null) return;
        weekTimelineList.removeAllViews();

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(weekStartMillis);

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

            itemView.findViewById(R.id.timeline_dot).setBackgroundColor(getResources().getColor(R.color.accent, null));
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

    // ===== 日视图导航 =====
    private void navigatePrevDay() {
        long nextDayMillis = currentDayMillis - 24L * 60 * 60 * 1000;
        // 允许切换到数据范围内的所有日期（包括空数据）
        if (nextDayMillis >= firstDataDayMillis && nextDayMillis <= lastDataDayMillis) {
            currentDayMillis = nextDayMillis;
            updateDayView();
        }
    }

    private void navigateNextDay() {
        long nextDayMillis = currentDayMillis + 24L * 60 * 60 * 1000;
        if (nextDayMillis <= lastDataDayMillis) {
            currentDayMillis = nextDayMillis;
            updateDayView();
        }
    }

    private void updateDayTitle() {
        if (dayTitle == null) return;

        String dateStr = DateUtils.formatDate(currentDayMillis);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(currentDayMillis);
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        String dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1];
        dayTitle.setText(dateStr + "（" + dayOfWeek + "）");

        // 更新日视图箭头状态
        boolean canGoPrev = currentDayMillis > firstDataDayMillis;
        boolean canGoNext = currentDayMillis < lastDataDayMillis;

        if (dayPrev != null) {
            dayPrev.setAlpha(canGoPrev ? 1.0f : 0.3f);
            dayPrev.setClickable(canGoPrev);
            dayPrev.setEnabled(canGoPrev);
        }
        if (dayNext != null) {
            dayNext.setAlpha(canGoNext ? 1.0f : 0.3f);
            dayNext.setClickable(canGoNext);
            dayNext.setEnabled(canGoNext);
        }
    }

    // ===== 周视图导航 =====
    private void navigatePrevWeek() {
        // 计算最早数据所在周
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setTimeInMillis(firstDataDayMillis);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long firstDataWeekStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());
        
        // 计算最晚数据所在周
        cal.setTimeInMillis(lastDataDayMillis);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long lastDataWeekStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());

        long nextWeekMillis = currentWeekStartMillis - 7L * 24 * 60 * 60 * 1000;
        // 允许切换到数据范围内的所有周（包括空数据）
        if (nextWeekMillis >= firstDataWeekStart && nextWeekMillis <= lastDataWeekStart) {
            currentWeekStartMillis = nextWeekMillis;
            updateWeekView();
        }
    }

    private void navigateNextWeek() {
        // 计算最晚数据所在周
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setTimeInMillis(lastDataDayMillis);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long lastDataWeekStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());

        long nextWeekMillis = currentWeekStartMillis + 7L * 24 * 60 * 60 * 1000;
        if (nextWeekMillis <= lastDataWeekStart) {
            currentWeekStartMillis = nextWeekMillis;
            updateWeekView();
        }
    }

    private void updateWeekTitle() {
        if (weekTitle == null) return;

        long weekEndMillis = currentWeekStartMillis + 6L * 24 * 60 * 60 * 1000;
        String startStr = DateUtils.formatDate(currentWeekStartMillis);
        String endStr = DateUtils.formatDate(weekEndMillis);
        weekTitle.setText(startStr + "~" + endStr);

        // 更新周视图箭头状态
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        
        // 计算最早数据所在周
        cal.setTimeInMillis(firstDataDayMillis);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long firstDataWeekStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());
        
        // 计算最晚数据所在周
        cal.setTimeInMillis(lastDataDayMillis);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        long lastDataWeekStart = DateUtils.getDayStartMillis(cal.getTimeInMillis());

        boolean canGoPrev = currentWeekStartMillis > firstDataWeekStart;
        boolean canGoNext = currentWeekStartMillis < lastDataWeekStart;

        if (weekPrev != null) {
            weekPrev.setAlpha(canGoPrev ? 1.0f : 0.3f);
            weekPrev.setClickable(canGoPrev);
            weekPrev.setEnabled(canGoPrev);
        }
        if (weekNext != null) {
            weekNext.setAlpha(canGoNext ? 1.0f : 0.3f);
            weekNext.setClickable(canGoNext);
            weekNext.setEnabled(canGoNext);
        }
    }

    // ===== 月视图导航 =====
    private void navigatePrevMonth() {
        // 计算上一个月的日期
        Calendar cal = Calendar.getInstance();
        cal.set(currentMonthYear, currentMonth, 1);
        cal.add(Calendar.MONTH, -1);
        int prevYear = cal.get(Calendar.YEAR);
        int prevMonth = cal.get(Calendar.MONTH);

        // 计算最早数据所在月份
        Calendar firstDataCal = Calendar.getInstance();
        firstDataCal.setTimeInMillis(firstDataDayMillis);
        int firstDataYear = firstDataCal.get(Calendar.YEAR);
        int firstDataMonth = firstDataCal.get(Calendar.MONTH);
        
        // 计算最晚数据所在月份
        Calendar lastDataCal = Calendar.getInstance();
        lastDataCal.setTimeInMillis(lastDataDayMillis);
        int lastDataYear = lastDataCal.get(Calendar.YEAR);
        int lastDataMonth = lastDataCal.get(Calendar.MONTH);

        // 允许切换到数据范围内的所有月份（包括空数据）
        boolean isAfterFirstData = prevYear > firstDataYear || (prevYear == firstDataYear && prevMonth >= firstDataMonth);
        boolean isBeforeLastData = prevYear < lastDataYear || (prevYear == lastDataYear && prevMonth <= lastDataMonth);

        if (isAfterFirstData && isBeforeLastData) {
            currentMonthYear = prevYear;
            currentMonth = prevMonth;
            updateMonthView();
        }
    }

    private void navigateNextMonth() {
        int nextMonth = currentMonth + 1;
        int nextYear = currentMonthYear;
        if (nextMonth > 11) {
            nextMonth = 0;
            nextYear += 1;
        }

        // 计算最晚数据所在月份
        Calendar lastDataCal = Calendar.getInstance();
        lastDataCal.setTimeInMillis(lastDataDayMillis);
        int lastDataYear = lastDataCal.get(Calendar.YEAR);
        int lastDataMonth = lastDataCal.get(Calendar.MONTH);

        // 检查是否在数据范围内
        boolean isBeforeLastData = nextYear < lastDataYear || (nextYear == lastDataYear && nextMonth <= lastDataMonth);
        if (isBeforeLastData) {
            currentMonth = nextMonth;
            currentMonthYear = nextYear;
            updateMonthView();
        }
    }

    private void updateMonthTitle() {
        if (monthTitle == null) return;
        monthTitle.setText(currentMonthYear + "年" + (currentMonth + 1) + "月");

        // 更新月视图箭头状态
        Calendar cal = Calendar.getInstance();
        
        // 计算最早数据所在月份
        cal.setTimeInMillis(firstDataDayMillis);
        int firstDataYear = cal.get(Calendar.YEAR);
        int firstDataMonth = cal.get(Calendar.MONTH);
        
        // 计算最晚数据所在月份
        cal.setTimeInMillis(lastDataDayMillis);
        int lastDataYear = cal.get(Calendar.YEAR);
        int lastDataMonth = cal.get(Calendar.MONTH);

        boolean canGoPrev = !(currentMonthYear == firstDataYear && currentMonth == firstDataMonth);
        boolean canGoNext = !(currentMonthYear == lastDataYear && currentMonth == lastDataMonth);

        if (monthPrev != null) {
            monthPrev.setAlpha(canGoPrev ? 1.0f : 0.3f);
            monthPrev.setClickable(canGoPrev);
            monthPrev.setEnabled(canGoPrev);
        }
        if (monthNext != null) {
            monthNext.setAlpha(canGoNext ? 1.0f : 0.3f);
            monthNext.setClickable(canGoNext);
            monthNext.setEnabled(canGoNext);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (openFileLauncher != null) {
            openFileLauncher.unregister();
        }
        // 注销数据变化监听
        if (dataManager != null) {
            dataManager.removeOnDataChangedListener(this::updateDataRange);
        }
    }
}