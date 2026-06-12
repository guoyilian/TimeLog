package com.example.timer;

import android.view.View;
import android.widget.TextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TagRecommender {

    public interface OnTagSelectedListener {
        void onTagSelected(String tag);
    }

    private static final int MIN_FREQUENCY = 5;
    private static final int MAX_TAGS = 4;
    private static final String[] DEFAULT_TAGS = {"学习", "工作", "看书", "运动"};

    public static List<String> getRecommendedTags(List<TimerRecord> records) {
        Map<String, Integer> nameCountMap = new HashMap<>();

        for (TimerRecord record : records) {
            String name = record.getName();
            if (!"REC".equals(name)) {
                nameCountMap.put(name, nameCountMap.getOrDefault(name, 0) + 1);
            }
        }

        List<Map.Entry<String, Integer>> sortedEntries = new ArrayList<>(nameCountMap.entrySet());
        Collections.sort(sortedEntries, (a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> frequentNames = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : sortedEntries) {
            if (entry.getValue() >= MIN_FREQUENCY) {
                frequentNames.add(entry.getKey());
            }
        }

        List<String> result = new ArrayList<>();
        for (int i = 0; i < MAX_TAGS; i++) {
            String tagText;
            if (i < frequentNames.size()) {
                tagText = frequentNames.get(i);
            } else {
                int defaultIndex = i - frequentNames.size();
                String usedDefault = DEFAULT_TAGS[defaultIndex];
                while (frequentNames.contains(usedDefault) && defaultIndex + 1 < DEFAULT_TAGS.length) {
                    defaultIndex++;
                    usedDefault = DEFAULT_TAGS[defaultIndex];
                }
                tagText = usedDefault;
            }
            result.add(tagText);
        }

        return result;
    }

    public static void applyToViews(List<TextView> tagViews, List<String> tags, OnTagSelectedListener listener) {
        for (int i = 0; i < tagViews.size() && i < tags.size(); i++) {
            TextView tagView = tagViews.get(i);
            String tagText = tags.get(i);
            tagView.setText(tagText);
            tagView.setVisibility(View.VISIBLE);
            tagView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onTagSelected(tagText);
                }
            });
        }
    }
}
