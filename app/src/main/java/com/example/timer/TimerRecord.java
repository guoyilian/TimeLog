package com.example.timer;

import java.io.Serializable;
import java.util.UUID;

public class TimerRecord implements Serializable {
    private String id;
    private String name;
    private String start;
    private String end;
    private String duration;
    private int durationMin;

    public TimerRecord() {
        this.id = UUID.randomUUID().toString();
    }

    public TimerRecord(String name, String start, String end, String duration, int durationMin) {
        this.id = UUID.randomUUID().toString();
        this.name = name;
        this.start = start;
        this.end = end;
        this.duration = duration;
        this.durationMin = durationMin;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStart() { return start; }
    public void setStart(String start) { this.start = start; }
    public String getEnd() { return end; }
    public void setEnd(String end) { this.end = end; }
    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }
    public int getDurationMin() { return durationMin; }
    public void setDurationMin(int durationMin) { this.durationMin = durationMin; }
}