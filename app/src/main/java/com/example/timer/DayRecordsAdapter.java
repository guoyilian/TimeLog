package com.example.timer;

import android.os.Handler;
import android.os.Looper;
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

    public interface OnItemNameClickListener {
        void onItemNameClick(int position, TimerRecord record);
    }

    private List<TimerRecord> records = new ArrayList<>();
    private OnItemDeleteListener deleteListener;
    private OnItemNameClickListener nameClickListener;
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

    public void setOnItemNameClickListener(OnItemNameClickListener listener) {
        this.nameClickListener = listener;
    }

    public void setRecords(List<TimerRecord> records) {
        this.records = new ArrayList<>(records);
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
        long start = record.getStart();
        long end = record.getEnd();
        holder.timeRange.setText(DateUtils.formatTime(start) + " - " + DateUtils.formatTime(end));
        int minutes = record.getDurationMin();
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0) {
            holder.duration.setText(hours + "小时" + mins + "分钟");
        } else {
            holder.duration.setText(mins + "分钟");
        }

        holder.dot.setBackgroundColor(position == 0 ? accentColor : accentLightColor);

        holder.name.setOnClickListener(v -> {
            if (nameClickListener != null && !holder.swipeItemView.isSwiping()) {
                final int originalColor = holder.name.getCurrentTextColor();
                holder.name.setTextColor(0xFF81C784);
                
                final int currentPos = holder.getAdapterPosition() == -1 ? holder.currentPosition : holder.getAdapterPosition();
                
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    holder.name.setTextColor(originalColor);
                    if (currentPos >= 0 && currentPos < records.size()) {
                        nameClickListener.onItemNameClick(currentPos, records.get(currentPos));
                    }
                }, 300);
            }
        });

        holder.swipeItemView.setOnDeleteClickListener(() -> {
            if (deleteListener != null) {
                int currentPos = holder.getAdapterPosition();
                if (currentPos == -1) {
                    currentPos = holder.currentPosition;
                }
                if (currentPos >= 0 && currentPos < records.size()) {
                    deleteListener.onItemDelete(currentPos);
                }
            }
        });

        holder.currentPosition = position;
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
        int currentPosition = -1;

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
