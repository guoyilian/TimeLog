package com.example.timer;

import java.util.ArrayList;
import java.util.List;

public class TimerRecordSplitter {

    public static List<TimerRecord> splitRecord(String name, long start, long end, int totalMinutes) {
        List<TimerRecord> records = new ArrayList<>();

        long startDayStart = DateUtils.getDayStartMillis(start);
        long endDayStart = DateUtils.getDayStartMillis(end);

        if (startDayStart == endDayStart) {
            records.add(new TimerRecord(name, start, end, DateUtils.formatDuration(totalMinutes), totalMinutes));
            return records;
        }

        long currentDayStart = startDayStart;
        long segmentStart = start;

        while (currentDayStart <= endDayStart) {
            long nextDayStart = currentDayStart + 24L * 60 * 60 * 1000;
            long segmentEnd;

            if (nextDayStart > end) {
                segmentEnd = end;
            } else {
                segmentEnd = nextDayStart;
            }

            long segmentDurationMillis = segmentEnd - segmentStart;
            int segmentDurationMin = Math.max(1, (int) Math.round(segmentDurationMillis / 60000f));

            records.add(new TimerRecord(
                    name,
                    segmentStart,
                    segmentEnd,
                    DateUtils.formatDuration(segmentDurationMin),
                    segmentDurationMin
            ));

            if (segmentEnd >= end) {
                break;
            }

            currentDayStart = nextDayStart;
            segmentStart = nextDayStart;
        }

        return records;
    }
}
