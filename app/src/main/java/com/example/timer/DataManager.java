package com.example.timer;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class DataManager {
    private static final String PREFS_NAME = "TimerRecords";
    private static final String RECORDS_KEY = "records";
    private SharedPreferences prefs;
    private Gson gson;

    public DataManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public List<TimerRecord> getRecords() {
        String json = prefs.getString(RECORDS_KEY, "[]");
        Type type = new TypeToken<ArrayList<TimerRecord>>() {}.getType();
        return gson.fromJson(json, type);
    }

    public void saveRecords(List<TimerRecord> records) {
        String json = gson.toJson(records);
        prefs.edit().putString(RECORDS_KEY, json).apply();
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
            if (r.getId() != null && r.getId().equals(record.getId())) {
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
            if (r.getId() != null && r.getId().equals(updatedRecord.getId())) {
                records.set(i, updatedRecord);
                break;
            }
        }
        saveRecords(records);
    }
}
