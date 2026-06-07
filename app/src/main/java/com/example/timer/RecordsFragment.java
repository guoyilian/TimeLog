package com.example.timer;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.animation.OvershootInterpolator;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.animation.Animator;
import android.os.Handler;
import android.os.Looper;
import android.animation.AnimatorListenerAdapter;
import android.view.ViewPropertyAnimator;

public class RecordsFragment extends Fragment {
    private DataManager dataManager;
    private RecyclerView dayTimelineList;
    private LinearLayout weekTimelineList;
    private TextView listHeader, chartListHeader;
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
    private com.example.timer.SolidPieChartView chartStatsPieChart;
    
    private String selectedDate; // 选中的日期 yyyy-MM-dd，null 表示今天
    private static final String[] TASK_COLORS = {
        "#6A9974", "#998A6A", "#996A6A", "#6A7A99", 
        "#7A6A99", "#996A8A", "#8A996A", "#6A998A"
    };

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
                                listHeader.setVisibility(View.GONE);
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
            showOperationBottomSheet(position, record);
        });
        dayTimelineList.setLayoutManager(new LinearLayoutManager(requireContext()));
        dayTimelineList.setAdapter(dayAdapter);

        weekTimelineList = view.findViewById(R.id.week_timeline_list);
        listHeader = view.findViewById(R.id.list_header);
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
        fabAdd.setOnClickListener(v -> showAddRecordDialog());

        subTabDay = view.findViewById(R.id.sub_tab_day);
        subTabWeek = view.findViewById(R.id.sub_tab_week);
        subTabMonth = view.findViewById(R.id.sub_tab_month);
        subTabYear = view.findViewById(R.id.sub_tab_year);

        dayLineChart = view.findViewById(R.id.day_line_chart);
        weekBarChart = view.findViewById(R.id.week_bar_chart);
        monthHeatmap = view.findViewById(R.id.month_heatmap);
        yearStatisticsView = view.findViewById(R.id.year_statistics_view);

        if (monthHeatmap != null) {
            monthHeatmap.setOnCellClickListener((date, day, minutes) -> showMonthDayDetail(date, minutes));
        }

        if (yearStatisticsView != null) {
            yearStatisticsView.setOnMonthClickListener((year, month) -> {
                switchSubView("month", year, month, true);
            });
        }

        if (weekBarChart != null) {
            weekBarChart.setOnBarClickListener((dayData, index) -> {
                selectedDate = dayData.fullDate;
                switchSubView("day", -1, -1, true);
            });
        }

        subTabDay.setOnClickListener(v -> {
            selectedDate = null; // 点击日标签时重置为今天
            switchSubView("day");
        });
        subTabWeek.setOnClickListener(v -> switchSubView("week"));
        subTabMonth.setOnClickListener(v -> switchSubView("month"));
        subTabYear.setOnClickListener(v -> switchSubView("year"));

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

    private static final long ANIMATION_DURATION = 300;
    private boolean isAnimating = false;

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
                clearMonthDayList();
            } else if (view.equals("year")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.VISIBLE);
                chartTaskStatsCard.setVisibility(View.GONE);
                renderYearHeatmap();
                clearMonthDayList();
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
                clearMonthDayList();
            } else if (view.equals("year")) {
                weekChartCard.setVisibility(View.GONE);
                monthHeatmapCard.setVisibility(View.GONE);
                yearHeatmapCard.setVisibility(View.VISIBLE);
                chartTaskStatsCard.setVisibility(View.GONE);
                renderYearHeatmap();
                clearMonthDayList();
            }
        }

        newView.setAlpha(1f);
        newView.setTranslationX(0);
        newView.setVisibility(View.VISIBLE);
    }

    private void updateDayView() {
        List<TimerRecord> dayRecords = getRecordsByDate(selectedDate);
        
        if (dayLineChart != null) {
            // 设置折线图标题
            if (selectedDate == null) {
                // 今天
                Calendar today = Calendar.getInstance();
                String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
                String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                String dayOfWeek = weekDays[today.get(Calendar.DAY_OF_WEEK) - 1];
                dayLineChart.setTitlePrefix(todayStr + "（" + dayOfWeek + "）");
            } else {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    Date date = sdf.parse(selectedDate);
                    Calendar cal = Calendar.getInstance();
                    cal.setTime(date);
                    
                    String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                    String dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1];
                    
                    dayLineChart.setTitlePrefix(selectedDate + "（" + dayOfWeek + "）");
                } catch (Exception e) {
                    dayLineChart.setTitlePrefix(selectedDate);
                }
            }
            dayLineChart.setData(dayRecords);
        }
        
        renderDayList(dayRecords);
    }

    private void updateDayChart() {
        if (dayLineChart != null) {
            dayLineChart.setData(getRecordsByDate(selectedDate));
        }
    }

    private List<TimerRecord> getRecordsByDate(String dateStr) {
        List<TimerRecord> result = new ArrayList<>();
        List<TimerRecord> allRecords = dataManager.getRecords();
        String targetDate = dateStr;
        if (targetDate == null) {
            targetDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
        }

        for (TimerRecord record : allRecords) {
            if (record.getStart().startsWith(targetDate)) {
                result.add(record);
            }
        }
        return result;
    }

    private void renderWeekChart() {
        if (weekBarChart == null) return;

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);

        String[] weekDays = {"周一", "周二", "周三", "周四", "周五", "周六", "周日"};
        List<WeekBarChartView.DayData> dayDataList = new ArrayList<>();
        List<TimerRecord> weekRecords = new ArrayList<>();

        List<TimerRecord> allRecords = dataManager.getRecords();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat dateFormat = new SimpleDateFormat("M/d", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            int dayMinutes = 0;
            String taskType = "";

            for (TimerRecord record : allRecords) {
                if (record.getStart().startsWith(dateStr)) {
                    dayMinutes += record.getDurationMin();
                    weekRecords.add(record);
                    if (taskType.isEmpty() && record.getName() != null) {
                        taskType = record.getName();
                    }
                }
            }

            String dateLabel = dateFormat.format(cal.getTime());
            dayDataList.add(new WeekBarChartView.DayData(weekDays[i], dateLabel, dateStr, dayMinutes, taskType));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        weekBarChart.setData(dayDataList);
        renderChartTaskStats(weekRecords, "本周统计");
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
        String monthPrefix = String.format("%04d-%02d", displayYear, displayMonth);

        for (TimerRecord record : allRecords) {
            if (record.getStart().startsWith(monthPrefix)) {
                String dateKey = record.getStart().substring(8, 10);
                int dayIndex = Integer.parseInt(dateKey) - 1;
                if (dayIndex >= 0 && dayIndex < 31) {
                    dailyMinutes[dayIndex] += record.getDurationMin();
                }
                monthRecords.add(record);
            }
        }

        monthHeatmap.setMonthData(dailyMinutes, firstDayOfWeek, displayYear, displayMonth);
        renderChartTaskStats(monthRecords, "本月统计");
    }

    private void clearMonthDayList() {
        if (weekTimelineList != null) {
            weekTimelineList.removeAllViews();
        }
        if (chartListHeader != null) {
            chartListHeader.setVisibility(View.GONE);
        }
    }

    private void renderYearHeatmap() {
        if (yearStatisticsView != null) {
            yearStatisticsView.updateData(dataManager.getRecords());
        }
    }

    private void showMonthDayDetail(String date, int minutes) {
        if (weekTimelineList == null) return;

        weekTimelineList.removeAllViews();
        if (chartListHeader != null) {
            chartListHeader.setVisibility(View.VISIBLE);
            chartListHeader.setText(date);
        }

        View itemView = LayoutInflater.from(requireContext()).inflate(R.layout.timeline_item, null);
        TextView name = itemView.findViewById(R.id.record_name);
        TextView timeRange = itemView.findViewById(R.id.record_time);
        TextView duration = itemView.findViewById(R.id.record_duration);
        TextView gotoDayBtn = itemView.findViewById(R.id.goto_day_btn);

        if (minutes <= 0) {
            name.setText(date);
            timeRange.setText("该天无记录");
            duration.setText("");
            gotoDayBtn.setVisibility(View.GONE);
        } else {
            name.setText(date);
            timeRange.setText("当日计时");

            int hours = minutes / 60;
            int mins = minutes % 60;
            if (hours > 0) {
                duration.setText(hours + "小时" + mins + "分钟");
            } else {
                duration.setText(mins + "分钟");
            }
            gotoDayBtn.setVisibility(View.VISIBLE);
            gotoDayBtn.setOnClickListener(v -> {
                selectedDate = date;
                switchSubView("day", -1, -1, true);
            });
        }

        itemView.findViewById(R.id.timeline_dot).setBackgroundColor(getResources().getColor(R.color.accent));
        itemView.findViewById(R.id.delete_btn).setVisibility(View.GONE);

        // 只有有数据时才添加点击事件
        if (minutes > 0) {
            View clickableContent = itemView.findViewById(R.id.clickable_content);
            clickableContent.setOnClickListener(v -> {
                String[] parts = date.split("-");
                String dayOfWeek = getDayOfWeek(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
                String displayDate = parts[1] + "/" + parts[2];
                showDayPieChartDialog(date, dayOfWeek, displayDate, true);
            });
        } else {
            // 没有数据时禁用点击效果
            View clickableContent = itemView.findViewById(R.id.clickable_content);
            clickableContent.setClickable(false);
            clickableContent.setEnabled(false);
        }

        weekTimelineList.addView(itemView);
    }

    private String getDayOfWeek(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekDays[dayOfWeek - 1];
    }

    private void renderDayList(List<TimerRecord> records) {
        // 不管记录是否为空，都设置列表数据
        if (selectedDate == null) {
            listHeader.setText("今日记录");
        } else {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date date = sdf.parse(selectedDate);
                Calendar cal = Calendar.getInstance();
                cal.setTime(date);
                
                String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
                String dayOfWeek = weekDays[cal.get(Calendar.DAY_OF_WEEK) - 1];
                SimpleDateFormat dateFormat = new SimpleDateFormat("M/d", Locale.getDefault());
                String dateLabel = dateFormat.format(date);
                
                listHeader.setText(dayOfWeek + " (" + dateLabel + ")");
            } catch (Exception e) {
                listHeader.setText(selectedDate);
            }
        }
        
        if (records.isEmpty()) {
            listHeader.setVisibility(View.GONE);
        } else {
            listHeader.setVisibility(View.VISIBLE);
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
        String[] dayDateStrings = new String[7];  // 保存日期字符串

        List<TimerRecord> allRecords = dataManager.getRecords();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        SimpleDateFormat displayDateFormat = new SimpleDateFormat("M/d", Locale.getDefault());

        for (int i = 0; i < 7; i++) {
            String dateStr = sdf.format(cal.getTime());
            dayDateStrings[i] = dateStr;  // 保存日期字符串
            dayDates[i] = displayDateFormat.format(cal.getTime());
            for (TimerRecord record : allRecords) {
                if (record.getStart().startsWith(dateStr)) {
                    dayMinutes[i] += record.getDurationMin();
                }
            }
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        listHeader.setVisibility(View.VISIBLE);
        listHeader.setText("本周记录");
        if (chartListHeader != null) {
            chartListHeader.setVisibility(View.VISIBLE);
            chartListHeader.setText("本周记录");
        }

        // 倒序遍历，从周日（索引6）到周一（索引0）
        for (int i = 6; i >= 0; i--) {
            if (dayMinutes[i] == 0) continue;

            final int dayIndex = i;
            final String dateStr = dayDateStrings[i];  // 使用保存的日期字符串

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

            // 绑定点击事件到内层的 clickable_content
            View clickableContent = itemView.findViewById(R.id.clickable_content);
            clickableContent.setOnClickListener(v -> showDayPieChartDialog(dateStr, weekDays[dayIndex], dayDates[dayIndex], false));

            weekTimelineList.addView(itemView);
        }

        if (weekTimelineList.getChildCount() == 0) {
            if (chartListHeader != null) {
                chartListHeader.setVisibility(View.GONE);
            }
        }
    }

    private void showDayPieChartDialog(String dateStr, String dayOfWeek, String displayDate, boolean isFromMonth) {
        List<TimerRecord> allRecords = dataManager.getRecords();
        Map<String, Integer> taskMinutes = new HashMap<>();

        for (TimerRecord record : allRecords) {
            if (record.getStart().startsWith(dateStr)) {
                String name = record.getName();
                if (name == null || name.isEmpty()) {
                    name = "未命名";
                }
                taskMinutes.put(name, taskMinutes.getOrDefault(name, 0) + record.getDurationMin());
            }
        }

        List<PieChartView.Slice> slices = new ArrayList<>();
        int totalMinutes = 0;
        for (Map.Entry<String, Integer> entry : taskMinutes.entrySet()) {
            slices.add(new PieChartView.Slice(entry.getKey(), entry.getValue()));
            totalMinutes += entry.getValue();
        }

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_pie_chart, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.getWindow().setLayout(
                    getResources().getDisplayMetrics().widthPixels * 9 / 10,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView subtitle = dialogView.findViewById(R.id.dialog_subtitle);
        TextView totalTime = dialogView.findViewById(R.id.dialog_total_time);
        TextView taskDetails = dialogView.findViewById(R.id.dialog_task_details);
        PieChartView pieChart = dialogView.findViewById(R.id.pie_chart);

        if (isFromMonth) {
            // 月视图：标题显示完整日期，隐藏副标题
            title.setText(dateStr);
            subtitle.setVisibility(View.GONE);
        } else {
            // 周视图：标题显示星期 + 日期，隐藏副标题
            title.setText(dayOfWeek + " (" + displayDate + ")");
            subtitle.setVisibility(View.GONE);
        }
        pieChart.setData(slices);

        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours > 0) {
            totalTime.setText("总计时：" + hours + "小时" + mins + "分钟");
        } else {
            totalTime.setText("总计时：" + mins + "分钟");
        }

        // 构建任务详情文本
        StringBuilder detailsBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : taskMinutes.entrySet()) {
            String taskName = entry.getKey();
            int taskMins = entry.getValue();
            
            // 添加分隔符（如果不是第一个）
            if (detailsBuilder.length() > 0) {
                detailsBuilder.append("；");
            }
            
            // 添加任务名称和时长
            detailsBuilder.append(taskName);
            detailsBuilder.append("：");
            int taskHours = taskMins / 60;
            int taskRemainingMins = taskMins % 60;
            if (taskHours > 0) {
                detailsBuilder.append(taskHours).append("小时").append(taskRemainingMins).append("分钟");
            } else {
                detailsBuilder.append(taskRemainingMins).append("分钟");
            }
        }
        
        // 设置任务详情文本
        taskDetails.setText(detailsBuilder.toString());
        // 如果有内容则显示，否则隐藏
        taskDetails.setVisibility(detailsBuilder.length() > 0 ? View.VISIBLE : View.GONE);

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    private void showOperationBottomSheet(int position, TimerRecord record) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(requireContext(), R.style.TransparentDialogStyle);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.bottom_sheet_operation, null);
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnEditName = dialogView.findViewById(R.id.btn_edit_name);
        Button btnEditTime = dialogView.findViewById(R.id.btn_edit_time);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnEditName.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    bottomSheetDialog.dismiss();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showEditNameDialog(position, record);
                    }, 100);
                })
                .start();
        });

        btnEditTime.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    bottomSheetDialog.dismiss();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showEditTimeDialog(position, record);
                    }, 100);
                })
                .start();
        });

        btnCancel.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    bottomSheetDialog.dismiss();
                })
                .start();
        });

        bottomSheetDialog.show();
    }

    private void showEditNameDialog(int position, TimerRecord record) {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_edit_name, null);
        EditText editText = dialogView.findViewById(R.id.dialog_input);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        editText.setText(record.getName());
        editText.setSelection(record.getName() != null ? record.getName().length() : 0);
        editText.requestFocus();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm_active, null));
                    btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
                } else {
                    btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm, null));
                    btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (editText.getText().toString().trim().isEmpty()) {
            btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm, null));
            btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
        } else {
            btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm_active, null));
            btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
        }

        AlertDialog dialog = new AlertDialog.Builder(requireContext(), R.style.TransparentDialogStyle)
                .setView(dialogView)
                .create();

        dialogView.setScaleX(0.9f);
        dialogView.setScaleY(0.9f);
        dialogView.setAlpha(0f);

        btnCancel.setOnClickListener(v -> {
            dialogView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> dialog.dismiss())
                .start();
        });

        btnConfirm.setOnClickListener(v -> {
            String newName = editText.getText().toString().trim();
            if (newName.isEmpty()) {
                newName = "REC";
            }
            record.setName(newName);
            updateRecordAndRefresh(position, record);
            dialogView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(() -> dialog.dismiss())
                .start();
        });

        dialog.show();

        dialogView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .start();
    }

    private void showEditTimeDialog(int position, TimerRecord record) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
        Calendar startCal = Calendar.getInstance();
        Calendar endCal = Calendar.getInstance();

        try {
            Date startDate = sdf.parse(record.getStart());
            Date endDate = sdf.parse(record.getEnd());
            if (startDate != null) {
                startCal.setTime(startDate);
            }
            if (endDate != null) {
                endCal.setTime(endDate);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }

        int startHour = startCal.get(Calendar.HOUR_OF_DAY);
        int startMinute = startCal.get(Calendar.MINUTE);
        int endHour = endCal.get(Calendar.HOUR_OF_DAY);
        int endMinute = endCal.get(Calendar.MINUTE);

        BottomSheetDialog timePickerDialog = new BottomSheetDialog(requireContext(), R.style.TransparentDialogStyle);
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_time_range_picker, null);
        timePickerDialog.setContentView(dialogView);
        timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        android.widget.NumberPicker npStartHour = dialogView.findViewById(R.id.np_start_hour);
        android.widget.NumberPicker npStartMinute = dialogView.findViewById(R.id.np_start_minute);
        android.widget.NumberPicker npEndHour = dialogView.findViewById(R.id.np_end_hour);
        android.widget.NumberPicker npEndMinute = dialogView.findViewById(R.id.np_end_minute);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        // 配置开始时间选择器
        npStartHour.setMinValue(0);
        npStartHour.setMaxValue(23);
        npStartHour.setValue(startHour);
        npStartHour.setFormatter(value -> String.format("%02d", value));

        npStartMinute.setMinValue(0);
        npStartMinute.setMaxValue(59);
        npStartMinute.setValue(startMinute);
        npStartMinute.setFormatter(value -> String.format("%02d", value));

        // 配置结束时间选择器
        npEndHour.setMinValue(0);
        npEndHour.setMaxValue(23);
        npEndHour.setValue(endHour);
        npEndHour.setFormatter(value -> String.format("%02d", value));

        npEndMinute.setMinValue(0);
        npEndMinute.setMaxValue(59);
        npEndMinute.setValue(endMinute);
        npEndMinute.setFormatter(value -> String.format("%02d", value));

        btnCancel.setOnClickListener(v -> {
            dismissWithSlideAnimation(timePickerDialog, dialogView);
        });

        btnConfirm.setOnClickListener(v -> {
            int newStartHour = npStartHour.getValue();
            int newStartMinute = npStartMinute.getValue();
            int newEndHour = npEndHour.getValue();
            int newEndMinute = npEndMinute.getValue();

            long startMillis = getTimeMillis(newStartHour, newStartMinute);
            long endMillis = getTimeMillis(newEndHour, newEndMinute);

            if (endMillis <= startMillis) {
                Toast.makeText(requireContext(), "结束时间不能早于开始时间", Toast.LENGTH_SHORT).show();
                return;
            }

            String datePart = record.getStart().split(" ")[0];
            String newStart = datePart + " " + String.format("%02d:%02d", newStartHour, newStartMinute);
            String newEnd = datePart + " " + String.format("%02d:%02d", newEndHour, newEndMinute);

            int newDurationMin = (int) ((endMillis - startMillis) / (1000 * 60));
            String newDuration;
            int hours = newDurationMin / 60;
            int mins = newDurationMin % 60;
            if (hours > 0) {
                newDuration = hours + "小时" + mins + "分钟";
            } else {
                newDuration = mins + "分钟";
            }

            record.setStart(newStart);
            record.setEnd(newEnd);
            record.setDuration(newDuration);
            record.setDurationMin(newDurationMin);

            updateRecordAndRefresh(position, record);
            dismissWithSlideAnimation(timePickerDialog, dialogView);
        });

        timePickerDialog.show();
    }

    private void dismissWithSlideAnimation(BottomSheetDialog dialog, View dialogView) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dialogView.animate()
                    .translationY(dialogView.getHeight())
                    .alpha(0f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator(-3f))
                    .withEndAction(() -> {
                        dialog.dismiss();
                    })
                    .start();
        }, 400);
    }

    private long getTimeMillis(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
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

    private void showAddRecordDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_add_record, null);
        DatePicker dpDate = dialogView.findViewById(R.id.dp_date);
        EditText etName = dialogView.findViewById(R.id.et_name);
        NumberPicker npStartHour = dialogView.findViewById(R.id.np_start_hour);
        NumberPicker npStartMinute = dialogView.findViewById(R.id.np_start_minute);
        NumberPicker npEndHour = dialogView.findViewById(R.id.np_end_hour);
        NumberPicker npEndMinute = dialogView.findViewById(R.id.np_end_minute);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        dpDate.setMaxDate(System.currentTimeMillis());

        npStartHour.setMinValue(0);
        npStartHour.setMaxValue(23);
        npStartHour.setFormatter(value -> String.format("%02d", value));

        npStartMinute.setMinValue(0);
        npStartMinute.setMaxValue(59);
        npStartMinute.setFormatter(value -> String.format("%02d", value));

        npEndHour.setMinValue(0);
        npEndHour.setMaxValue(23);
        npEndHour.setFormatter(value -> String.format("%02d", value));

        npEndMinute.setMinValue(0);
        npEndMinute.setMaxValue(59);
        npEndMinute.setFormatter(value -> String.format("%02d", value));

        Calendar now = Calendar.getInstance();
        npStartHour.setValue(now.get(Calendar.HOUR_OF_DAY));
        npStartMinute.setValue(now.get(Calendar.MINUTE));

        int defaultEndHour = now.get(Calendar.HOUR_OF_DAY) + 1;
        if (defaultEndHour >= 24) defaultEndHour = 23;
        npEndHour.setValue(defaultEndHour);
        npEndMinute.setValue(now.get(Calendar.MINUTE));

        BottomSheetDialog dialog = new BottomSheetDialog(requireContext(), R.style.TransparentDialogStyle);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setSkipCollapsed(true);

        btnCancel.setOnClickListener(v -> {
            dismissWithSlideAnimation(dialog, dialogView);
        });

        btnConfirm.setOnClickListener(v -> {
            int year = dpDate.getYear();
            int month = dpDate.getMonth();
            int day = dpDate.getDayOfMonth();

            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                name = "REC";
            }

            int startHour = npStartHour.getValue();
            int startMinute = npStartMinute.getValue();
            int endHour = npEndHour.getValue();
            int endMinute = npEndMinute.getValue();

            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, day, startHour, startMinute, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.set(year, month, day, endHour, endMinute, 0);
            endCal.set(Calendar.MILLISECOND, 0);

            if (startCal.getTimeInMillis() > endCal.getTimeInMillis()) {
                Toast.makeText(requireContext(), "结束时间不能早于开始时间", Toast.LENGTH_SHORT).show();
                return;
            }

            String dateStr = String.format("%04d-%02d-%02d", year, month + 1, day);
            String startTimeStr = String.format("%04d-%02d-%02d %02d:%02d", year, month + 1, day, startHour, startMinute);
            String endTimeStr = String.format("%04d-%02d-%02d %02d:%02d", year, month + 1, day, endHour, endMinute);
            int durationMin = (int) ((endCal.getTimeInMillis() - startCal.getTimeInMillis()) / 60000);

            TimerRecord newRecord = new TimerRecord();
            newRecord.setName(name);
            newRecord.setStart(startTimeStr);
            newRecord.setEnd(endTimeStr);
            newRecord.setDuration(String.valueOf(durationMin));
            newRecord.setDurationMin(durationMin);

            dataManager.addRecord(newRecord);

            dismissWithSlideAnimation(dialog, dialogView);

            updateDayView();
        });

        dialog.show();
    }

    private void renderChartTaskStats(List<TimerRecord> records, String title) {
        if (chartStatsTagsLayout == null || chartStatsTitle == null || chartTaskStatsCard == null) {
            return;
        }

        chartStatsTagsLayout.removeAllViews();

        Map<String, Integer> taskMinutes = new HashMap<>();
        for (TimerRecord record : records) {
            String taskName = record.getName();
            int mins = record.getDurationMin();
            if (taskMinutes.containsKey(taskName)) {
                taskMinutes.put(taskName, taskMinutes.get(taskName) + mins);
            } else {
                taskMinutes.put(taskName, mins);
            }
        }

        if (taskMinutes.isEmpty()) {
            chartTaskStatsCard.setVisibility(View.GONE);
            return;
        }

        chartTaskStatsCard.setVisibility(View.VISIBLE);
        chartStatsTitle.setText(title);

        // 先创建排序后的列表（按时间从大到小）
        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(taskMinutes.entrySet());
        sortedList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        // 渲染饼图（使用排序后的相同顺序，确保颜色对应）
        if (chartStatsPieChart != null) {
            List<com.example.timer.SolidPieChartView.Slice> slices = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sortedList) {
                slices.add(new com.example.timer.SolidPieChartView.Slice(entry.getKey(), entry.getValue()));
            }
            chartStatsPieChart.setData(slices);
        }

        int colorIndex = 0;
        for (Map.Entry<String, Integer> entry : sortedList) {
            View tagView = LayoutInflater.from(requireContext()).inflate(R.layout.item_task_tag, chartStatsTagsLayout, false);

            View colorDot = tagView.findViewById(R.id.tag_color_dot);
            TextView tagText = tagView.findViewById(R.id.tag_text);

            // 使用与饼图相同的马卡龙色系
            int color = com.example.timer.SolidPieChartView.COLORS[colorIndex % com.example.timer.SolidPieChartView.COLORS.length];
            colorDot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));

            int mins = entry.getValue();
            int hours = mins / 60;
            int remainingMins = mins % 60;
            String durationText;
            if (hours > 0) {
                durationText = hours + "h" + remainingMins + "m";
            } else {
                durationText = remainingMins + "m";
            }

            tagText.setText(entry.getKey() + "：" + durationText);

            ViewGroup.MarginLayoutParams lp = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.rightMargin = dpToPx(8);
            lp.bottomMargin = dpToPx(8);
            tagView.setLayoutParams(lp);

            chartStatsTagsLayout.addView(tagView);
            colorIndex++;
        }
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density + 0.5f);
    }
}