package com.example.timer;

import android.app.Dialog;
import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RecordsChartHelper {

    public interface OnJumpToDayListener {
        void onJumpToDay(String date);
    }

    private final Context context;
    private final DataManager dataManager;
    private final OnJumpToDayListener jumpListener;
    private final TextView chartListHeader;
    private final LinearLayout weekTimelineList;
    private final com.example.timer.FlowLayout chartStatsTagsLayout;
    private final TextView chartStatsTitle;
    private final View chartTaskStatsCard;
    private final com.example.timer.PieChartView chartStatsPieChart;

    public RecordsChartHelper(Context context, DataManager dataManager,
                               OnJumpToDayListener jumpListener,
                               TextView chartListHeader,
                               LinearLayout weekTimelineList,
                               com.example.timer.FlowLayout chartStatsTagsLayout,
                               TextView chartStatsTitle,
                               View chartTaskStatsCard,
                               com.example.timer.PieChartView chartStatsPieChart) {
        this.context = context;
        this.dataManager = dataManager;
        this.jumpListener = jumpListener;
        this.chartListHeader = chartListHeader;
        this.weekTimelineList = weekTimelineList;
        this.chartStatsTagsLayout = chartStatsTagsLayout;
        this.chartStatsTitle = chartStatsTitle;
        this.chartTaskStatsCard = chartTaskStatsCard;
        this.chartStatsPieChart = chartStatsPieChart;
    }

    public void showMonthDayDetail(String date, int minutes) {
        if (weekTimelineList == null) return;

        weekTimelineList.removeAllViews();
        if (chartListHeader != null) {
            chartListHeader.setVisibility(View.VISIBLE);
            chartListHeader.setText(date);
        }

        View itemView = LayoutInflater.from(context).inflate(R.layout.timeline_item, null);
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
                if (jumpListener != null) {
                    jumpListener.onJumpToDay(date);
                }
            });
        }

        itemView.findViewById(R.id.timeline_dot).setBackgroundColor(context.getResources().getColor(R.color.accent));
        itemView.findViewById(R.id.delete_btn).setVisibility(View.GONE);

        if (minutes > 0) {
            View clickableContent = itemView.findViewById(R.id.clickable_content);
            clickableContent.setOnClickListener(v -> {
                String[] parts = date.split("-");
                String dayOfWeek = getDayOfWeek(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]) - 1, Integer.parseInt(parts[2]));
                String displayDate = parts[1] + "/" + parts[2];
                showDayPieChartDialog(date, dayOfWeek, displayDate, true);
            });
        } else {
            View clickableContent = itemView.findViewById(R.id.clickable_content);
            clickableContent.setClickable(false);
            clickableContent.setEnabled(false);
        }

        weekTimelineList.addView(itemView);
    }

    public void showDayPieChartDialog(String dateStr, String dayOfWeek, String displayDate, boolean isFromMonth) {
        List<TimerRecord> allRecords = dataManager.getRecords();
        Map<String, Integer> taskMinutes = new HashMap<>();

        String[] parts = dateStr.split("-");
        int year = Integer.parseInt(parts[0]);
        int month = Integer.parseInt(parts[1]) - 1;
        int day = Integer.parseInt(parts[2]);
        long dayStart = DateUtils.getSpecificDayStartMillis(year, month, day);
        long dayEnd = dayStart + 24L * 60 * 60 * 1000;

        for (TimerRecord record : allRecords) {
            long start = record.getStart();
            if (start >= dayStart && start < dayEnd) {
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

        Dialog dialog = new Dialog(context);
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_pie_chart, null);
        dialog.setContentView(dialogView);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().setGravity(Gravity.CENTER);
            dialog.getWindow().setLayout(
                    context.getResources().getDisplayMetrics().widthPixels * 9 / 10,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        TextView title = dialogView.findViewById(R.id.dialog_title);
        TextView subtitle = dialogView.findViewById(R.id.dialog_subtitle);
        TextView totalTime = dialogView.findViewById(R.id.dialog_total_time);
        TextView taskDetails = dialogView.findViewById(R.id.dialog_task_details);
        PieChartView pieChart = dialogView.findViewById(R.id.pie_chart);

        if (isFromMonth) {
            title.setText(dateStr);
            subtitle.setVisibility(View.GONE);
        } else {
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

        StringBuilder detailsBuilder = new StringBuilder();
        for (Map.Entry<String, Integer> entry : taskMinutes.entrySet()) {
            String taskName = entry.getKey();
            int taskMins = entry.getValue();

            if (detailsBuilder.length() > 0) {
                detailsBuilder.append("；");
            }

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

        taskDetails.setText(detailsBuilder.toString());
        taskDetails.setVisibility(detailsBuilder.length() > 0 ? View.VISIBLE : View.GONE);

        dialog.setCanceledOnTouchOutside(true);
        dialog.show();
    }

    public void renderChartTaskStats(List<TimerRecord> records, String title) {
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

        List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(taskMinutes.entrySet());
        sortedList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));

        if (chartStatsPieChart != null) {
            List<com.example.timer.PieChartView.Slice> slices = new ArrayList<>();
            for (Map.Entry<String, Integer> entry : sortedList) {
                slices.add(new com.example.timer.PieChartView.Slice(entry.getKey(), entry.getValue()));
            }
            chartStatsPieChart.setData(slices);
        }

        int colorIndex = 0;
        float density = context.getResources().getDisplayMetrics().density;
        for (Map.Entry<String, Integer> entry : sortedList) {
            View tagView = LayoutInflater.from(context).inflate(R.layout.item_task_tag, chartStatsTagsLayout, false);

            View colorDot = tagView.findViewById(R.id.tag_color_dot);
            TextView tagText = tagView.findViewById(R.id.tag_text);

            int color = com.example.timer.PieChartView.DEFAULT_SOLID_COLORS[colorIndex % com.example.timer.PieChartView.DEFAULT_SOLID_COLORS.length];
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
            lp.rightMargin = dpToPx(8, density);
            lp.bottomMargin = dpToPx(8, density);
            tagView.setLayoutParams(lp);

            chartStatsTagsLayout.addView(tagView);
            colorIndex++;
        }
    }

    public void clearMonthDayList() {
        if (weekTimelineList != null) {
            weekTimelineList.removeAllViews();
        }
        if (chartListHeader != null) {
            chartListHeader.setVisibility(View.GONE);
        }
    }

    private String getDayOfWeek(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekDays[dayOfWeek - 1];
    }

    private int dpToPx(int dp, float density) {
        return (int) (dp * density + 0.5f);
    }
}
