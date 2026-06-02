package com.example.timer;

import android.app.Dialog;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.view.ViewPropertyAnimator;

public class RecordsFragment extends Fragment {
    private DataManager dataManager;
    private RecyclerView dayTimelineList;
    private LinearLayout weekTimelineList;
    private TextView listHeader, chartListHeader;
    private View weekChartCard, monthHeatmapCard, yearHeatmapCard;
    private LinearLayout viewDay, viewChart;
    private Button subTabDay, subTabWeek, subTabMonth, subTabYear;
    private DayLineChartView dayLineChart;
    private WeekBarChartView weekBarChart;
    private MonthHeatmapView monthHeatmap;
    private YearStatisticsView yearStatisticsView;
    private DayRecordsAdapter dayAdapter;

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

        viewDay.setOnTouchListener((v, event) -> {
            dayAdapter.closeAllSwipeItems();
            return false;
        });

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

        subTabDay.setOnClickListener(v -> switchSubView("day"));
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
            updateDayView();
        } else {
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
            updateDayView();
        } else {
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
                renderYearHeatmap();
                clearMonthDayList();
            }
        }

        newView.setAlpha(1f);
        newView.setTranslationX(0);
        newView.setVisibility(View.VISIBLE);
    }

    private void updateDayView() {
        List<TimerRecord> dayRecords = getTodayRecords();
        if (dayLineChart != null) {
            dayLineChart.setData(dayRecords);
        }
        renderDayList(dayRecords);
    }

    private void updateDayChart() {
        if (dayLineChart != null) {
            dayLineChart.setData(getTodayRecords());
        }
    }

    private List<TimerRecord> getTodayRecords() {
        List<TimerRecord> result = new ArrayList<>();
        List<TimerRecord> allRecords = dataManager.getRecords();
        String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());

        for (TimerRecord record : allRecords) {
            if (record.getStart().startsWith(today)) {
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
                    if (taskType.isEmpty() && record.getName() != null) {
                        taskType = record.getName();
                    }
                }
            }

            String dateLabel = dateFormat.format(cal.getTime());
            dayDataList.add(new WeekBarChartView.DayData(weekDays[i], dateLabel, dayMinutes, taskType));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }

        weekBarChart.setData(dayDataList);
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
        int firstDayOfWeek = MonthHeatmapView.getMondayBasedDayOfWeek(cal);

        int[] dailyMinutes = new int[31];
        List<TimerRecord> allRecords = dataManager.getRecords();
        String monthPrefix = String.format("%04d-%02d", displayYear, displayMonth);

        for (TimerRecord record : allRecords) {
            if (record.getStart().startsWith(monthPrefix)) {
                String dateKey = record.getStart().substring(8, 10);
                int dayIndex = Integer.parseInt(dateKey) - 1;
                if (dayIndex >= 0 && dayIndex < 31) {
                    dailyMinutes[dayIndex] += record.getDurationMin();
                }
            }
        }

        monthHeatmap.setMonthData(dailyMinutes, firstDayOfWeek, displayYear, displayMonth);
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

        if (minutes <= 0) {
            name.setText(date);
            timeRange.setText("该天无记录");
            duration.setText("");
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
        if (records.isEmpty()) {
            listHeader.setVisibility(View.GONE);
            return;
        }

        listHeader.setVisibility(View.VISIBLE);
        listHeader.setText("今日记录");
        dayAdapter.setRecords(records);
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

        for (int i = 0; i < 7; i++) {
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

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }
}