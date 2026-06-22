package com.example.timer;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String PREFS_NAME = "TimerRecords";
    private static final String RECORDS_KEY = "records";
    private static final String STATE_KEY = "running_timer_state";
    private SharedPreferences prefs;
    private Gson gson;

    public DataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public RunningTimerState getRunningTimerState() {
        String json = prefs.getString(STATE_KEY, null);
        if (json == null) {
            return null;
        }
        try {
            return gson.fromJson(json, RunningTimerState.class);
        } catch (Exception e) {
            return null;
        }
    }

    public void saveRunningTimerState(RunningTimerState state) {
        String json = gson.toJson(state);
        prefs.edit().putString(STATE_KEY, json).apply();
    }

    public void clearRunningTimerState() {
        prefs.edit().remove(STATE_KEY).apply();
    }

    // 数据变化监听接口
    public interface OnDataChangedListener {
        void onDataChanged();
    }
    
    private List<OnDataChangedListener> dataChangedListeners = new ArrayList<>();
    
    public void addOnDataChangedListener(OnDataChangedListener listener) {
        dataChangedListeners.add(listener);
    }
    
    public void removeOnDataChangedListener(OnDataChangedListener listener) {
        dataChangedListeners.remove(listener);
    }
    
    private void notifyDataChanged() {
        for (OnDataChangedListener listener : dataChangedListeners) {
            listener.onDataChanged();
        }
    }

    public static class RunningTimerState implements Serializable {
        public long firstStartTime;
        public long elapsedTime;
        public long lastUpdateTime;
        public long lastUpdateElapsedRealtime;
        public boolean isRunning;
        public boolean isPaused;
        public String timerName;

        public RunningTimerState() {}

        public RunningTimerState(long firstStartTime, long elapsedTime, long lastUpdateTime,
                                 long lastUpdateElapsedRealtime,
                                 boolean isRunning, boolean isPaused, String timerName) {
            this.firstStartTime = firstStartTime;
            this.elapsedTime = elapsedTime;
            this.lastUpdateTime = lastUpdateTime;
            this.lastUpdateElapsedRealtime = lastUpdateElapsedRealtime;
            this.isRunning = isRunning;
            this.isPaused = isPaused;
            this.timerName = timerName;
        }
    }

    public List<TimerRecord> getRecords() {
        String json = prefs.getString(RECORDS_KEY, "[]");
        Type type = new TypeToken<ArrayList<TimerRecord>>() {}.getType();
        try {
            return gson.fromJson(json, type);
        } catch (JsonSyntaxException e) {
            return migrateFromLegacyFormat(json);
        }
    }

    private List<TimerRecord> migrateFromLegacyFormat(String json) {
        Type legacyType = new TypeToken<ArrayList<LegacyTimerRecord>>() {}.getType();
        try {
            List<LegacyTimerRecord> legacyRecords = gson.fromJson(json, legacyType);
            List<TimerRecord> newRecords = new ArrayList<>();
            for (LegacyTimerRecord legacy : legacyRecords) {
                long start = DateUtils.parseDateTime(legacy.start);
                long end = DateUtils.parseDateTime(legacy.end);
                TimerRecord record = new TimerRecord();
                record.setId(legacy.id);
                record.setName(legacy.name);
                record.setStart(start);
                record.setEnd(end);
                record.setDuration(legacy.duration);
                record.setDurationMin(legacy.durationMin);
                newRecords.add(record);
            }
            saveRecords(newRecords);
            return newRecords;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public void saveRecords(List<TimerRecord> records) {
        String json = gson.toJson(records);
        prefs.edit().putString(RECORDS_KEY, json).apply();
        notifyDataChanged();
    }

    public void addRecord(TimerRecord record) {
        List<TimerRecord> records = getRecords();
        records.add(0, record);
        saveRecords(records);
    }

    public int getTotalMinutes() {
        List<TimerRecord> records = getRecords();
        int total = 0;
        for (TimerRecord record : records) {
            total += record.getDurationMin();
        }
        return total;
    }

    public int getRecordCount() {
        return getRecords().size();
    }

    public void deleteRecord(TimerRecord record) {
        List<TimerRecord> records = getRecords();
        for (int i = 0; i < records.size(); i++) {
            TimerRecord r = records.get(i);
            if (java.util.Objects.equals(r.getId(), record.getId())) {
                records.remove(i);
                break;
            }
        }
        saveRecords(records);
    }

    public void updateRecord(TimerRecord updatedRecord) {
        List<TimerRecord> records = getRecords();
        for (int i = 0; i < records.size(); i++) {
            TimerRecord r = records.get(i);
            if (java.util.Objects.equals(r.getId(), updatedRecord.getId())) {
                records.set(i, updatedRecord);
                break;
            }
        }
        saveRecords(records);
    }

    public String exportRecordsToJson() {
        List<TimerRecord> records = getRecords();
        ExportData data = new ExportData();
        data.version = 1;
        data.exportedAt = System.currentTimeMillis();
        data.appVersion = "timer-android-v1";
        data.recordCount = records.size();
        data.records = records;
        return gson.toJson(data);
    }

    public int importRecordsFromJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return 0;
        }
        ExportData imported;
        try {
            Type type = new TypeToken<ExportData>() {}.getType();
            imported = gson.fromJson(json, type);
        } catch (Exception e) {
            return 0;
        }
        if (imported == null || imported.records == null || imported.records.isEmpty()) {
            return 0;
        }
        List<TimerRecord> existingRecords = getRecords();
        java.util.Set<String> existingIds = new java.util.HashSet<>();
        for (TimerRecord r : existingRecords) {
            if (r.getId() != null) {
                existingIds.add(r.getId());
            }
        }
        int addedCount = 0;
        for (TimerRecord r : imported.records) {
            if (r.getId() == null || r.getId().isEmpty()) {
                continue;
            }
            if (!existingIds.contains(r.getId())) {
                existingRecords.add(0, r);
                existingIds.add(r.getId());
                addedCount++;
            }
        }
        if (addedCount > 0) {
            saveRecords(existingRecords);
        }
        return addedCount;
    }

    private static class ExportData {
        int version;
        long exportedAt;
        String appVersion;
        int recordCount;
        List<TimerRecord> records;
    }

    private static class LegacyTimerRecord implements Serializable {
        String id;
        String name;
        String start;
        String end;
        String duration;
        int durationMin;
    }
}
