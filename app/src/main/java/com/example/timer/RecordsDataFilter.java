package com.example.timer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class RecordsDataFilter {

    public static List<TimerRecord> filterByDate(List<TimerRecord> allRecords, String dateStr) {
        List<TimerRecord> result = new ArrayList<>();
        long targetDayStart;
        if (dateStr == null) {
            targetDayStart = DateUtils.getDayStartMillis(System.currentTimeMillis());
        } else {
            String[] parts = dateStr.split("-");
            int year = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]) - 1;
            int day = Integer.parseInt(parts[2]);
            targetDayStart = DateUtils.getSpecificDayStartMillis(year, month, day);
        }
        long targetDayEnd = targetDayStart + 24L * 60 * 60 * 1000;

        for (TimerRecord record : allRecords) {
            long start = record.getStart();
            if (start >= targetDayStart && start < targetDayEnd) {
                result.add(record);
            }
        }
        return result;
    }

    public static String getDayOfWeekDisplay(int year, int month, int day) {
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String[] weekDays = {"周日", "周一", "周二", "周三", "周四", "周五", "周六"};
        return weekDays[dayOfWeek - 1];
    }
}
