package com.example.timer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DayRecordsAdapter extends RecyclerView.Adapter<DayRecordsAdapter.ViewHolder> implements SwipeItemView.OnSwipeStateChangeListener {

    public interface OnItemDeleteListener {
        void onItemDelete(int position);
    }

    private List<TimerRecord> records = new ArrayList<>();
    private OnItemDeleteListener deleteListener;
    private int accentColor;
    private int accentLightColor;
    private SwipeItemView lastOpenSwipeItem = null;

    public DayRecordsAdapter(int accentColor, int accentLightColor) {
        this.accentColor = accentColor;
        this.accentLightColor = accentLightColor;
    }

    public void setOnItemDeleteListener(OnItemDeleteListener listener) {
        this.deleteListener = listener;
    }

    public void setRecords(List<TimerRecord> records) {
        this.records = records;
        notifyDataSetChanged();
    }

    public List<TimerRecord> getRecords() {
        return records;
    }

    public void removeItem(int position) {
        if (position >= 0 && position < records.size()) {
            records.remove(position);
            notifyItemRemoved(position);
        }
    }

    private void closeLastOpenSwipeItem() {
        if (lastOpenSwipeItem != null) {
            lastOpenSwipeItem.closeSwipe();
            lastOpenSwipeItem = null;
        }
    }

    public void closeAllSwipeItems() {
        closeLastOpenSwipeItem();
    }

    public void closeSwipeItemAtPosition(int position) {
        // 关闭指定位置的滑动项（如果它是当前打开的）
        closeLastOpenSwipeItem();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_swipe, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TimerRecord record = records.get(position);

        holder.name.setText(record.getName());
        String start = record.getStart();
        String end = record.getEnd();
        if (start.contains(" ")) {
            start = start.split(" ")[1];
        }
        if (end.contains(" ")) {
            end = end.split(" ")[1];
        }
        holder.timeRange.setText(start + " - " + end);
        holder.duration.setText(record.getDuration());

        holder.dot.setBackgroundColor(position == 0 ? accentColor : accentLightColor);

        holder.swipeItemView.setOnDeleteClickListener((pos) -> {
            if (deleteListener != null) {
                deleteListener.onItemDelete(pos);
            }
        }, position);

        holder.swipeItemView.setOnSwipeStateChangeListener(this);
    }

    @Override
    public void onSwipeOpened(SwipeItemView item) {
        if (lastOpenSwipeItem != null && lastOpenSwipeItem != item) {
            lastOpenSwipeItem.closeSwipe();
        }
        lastOpenSwipeItem = item;
    }

    @Override
    public void onSwipeClosed(SwipeItemView item) {
        if (lastOpenSwipeItem == item) {
            lastOpenSwipeItem = null;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull ViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (lastOpenSwipeItem == holder.swipeItemView) {
            lastOpenSwipeItem = null;
        }
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView name;
        TextView timeRange;
        TextView duration;
        View dot;
        SwipeItemView swipeItemView;

        ViewHolder(View itemView) {
            super(itemView);
            swipeItemView = (SwipeItemView) itemView;
            name = itemView.findViewById(R.id.record_name);
            timeRange = itemView.findViewById(R.id.record_time);
            duration = itemView.findViewById(R.id.record_duration);
            dot = itemView.findViewById(R.id.timeline_dot);
        }
    }
}
