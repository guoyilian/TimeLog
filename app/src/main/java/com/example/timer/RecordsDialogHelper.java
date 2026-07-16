package com.example.timer;

import android.content.Context;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Editable;
import android.text.TextWatcher;
import android.os.Handler;
import android.os.Looper;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import java.util.Calendar;

public class RecordsDialogHelper {

    public interface OnRecordChangedListener {
        void onRecordAdded();
        void onRecordUpdated(int position, TimerRecord record);
    }

    private final Context context;
    private final DataManager dataManager;
    private final OnRecordChangedListener listener;

    public RecordsDialogHelper(Context context, DataManager dataManager, OnRecordChangedListener listener) {
        this.context = context;
        this.dataManager = dataManager;
        this.listener = listener;
    }

    public void showOperationBottomSheet(int position, TimerRecord record) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(context, R.style.TransparentDialogStyle);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_operation, null);
        bottomSheetDialog.setContentView(dialogView);
        bottomSheetDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnEditName = dialogView.findViewById(R.id.btn_edit_name);
        Button btnEditTime = dialogView.findViewById(R.id.btn_edit_time);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);

        btnEditName.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    bottomSheetDialog.dismiss();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showEditNameDialog(position, record);
                    }, 100);
                })
                .start();
        });

        btnEditTime.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(() -> {
                    bottomSheetDialog.dismiss();
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        showEditTimeDialog(position, record);
                    }, 100);
                })
                .start();
        });

        btnCancel.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(150)
                .withEndAction(bottomSheetDialog::dismiss)
                .start();
        });

        bottomSheetDialog.show();
    }

    public void showEditNameDialog(int position, TimerRecord record) {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_edit_name, null);
        EditText editText = dialogView.findViewById(R.id.dialog_input);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        editText.setText(record.getName());
        editText.setSelection(record.getName() != null ? record.getName().length() : 0);
        editText.requestFocus();

        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    btnConfirm.setBackground(context.getResources().getDrawable(R.drawable.ripple_btn_confirm_active, null));
                    btnConfirm.setTextColor(context.getResources().getColor(android.R.color.white, null));
                } else {
                    btnConfirm.setBackground(context.getResources().getDrawable(R.drawable.ripple_btn_confirm, null));
                    btnConfirm.setTextColor(context.getResources().getColor(android.R.color.white, null));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (editText.getText().toString().trim().isEmpty()) {
            btnConfirm.setBackground(context.getResources().getDrawable(R.drawable.ripple_btn_confirm, null));
            btnConfirm.setTextColor(context.getResources().getColor(android.R.color.white, null));
        } else {
            btnConfirm.setBackground(context.getResources().getDrawable(R.drawable.ripple_btn_confirm_active, null));
            btnConfirm.setTextColor(context.getResources().getColor(android.R.color.white, null));
        }

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.TransparentDialogStyle)
                .setView(dialogView)
                .create();

        dialogView.setScaleX(0.9f);
        dialogView.setScaleY(0.9f);
        dialogView.setAlpha(0f);

        btnCancel.setOnClickListener(v -> {
            dialogView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(dialog::dismiss)
                .start();
        });

        btnConfirm.setOnClickListener(v -> {
            String newName = editText.getText().toString().trim();
            if (newName.isEmpty()) {
                newName = "REC";
            }
            record.setName(newName);
            listener.onRecordUpdated(position, record);
            dialogView.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .alpha(0f)
                .setDuration(200)
                .withEndAction(dialog::dismiss)
                .start();
        });

        dialog.show();

        dialogView.animate()
            .scaleX(1f)
            .scaleY(1f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new OvershootInterpolator(1.1f))
            .start();
    }

    public void showEditTimeDialog(int position, TimerRecord record) {
        Calendar startCal = Calendar.getInstance();
        startCal.setTimeInMillis(record.getStart());
        Calendar endCal = Calendar.getInstance();
        endCal.setTimeInMillis(record.getEnd());

        int startHour = startCal.get(Calendar.HOUR_OF_DAY);
        int startMinute = startCal.get(Calendar.MINUTE);
        int endHour = endCal.get(Calendar.HOUR_OF_DAY);
        int endMinute = endCal.get(Calendar.MINUTE);

        BottomSheetDialog timePickerDialog = new BottomSheetDialog(context, R.style.TransparentDialogStyle);
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_time_range_picker, null);
        timePickerDialog.setContentView(dialogView);
        timePickerDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        NumberPicker npStartHour = dialogView.findViewById(R.id.np_start_hour);
        NumberPicker npStartMinute = dialogView.findViewById(R.id.np_start_minute);
        NumberPicker npEndHour = dialogView.findViewById(R.id.np_end_hour);
        NumberPicker npEndMinute = dialogView.findViewById(R.id.np_end_minute);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        npStartHour.setMinValue(0);
        npStartHour.setMaxValue(23);
        npStartHour.setValue(startHour);
        npStartHour.setFormatter(value -> String.format("%02d", value));

        npStartMinute.setMinValue(0);
        npStartMinute.setMaxValue(59);
        npStartMinute.setValue(startMinute);
        npStartMinute.setFormatter(value -> String.format("%02d", value));

        npEndHour.setMinValue(0);
        npEndHour.setMaxValue(23);
        npEndHour.setValue(endHour);
        npEndHour.setFormatter(value -> String.format("%02d", value));

        npEndMinute.setMinValue(0);
        npEndMinute.setMaxValue(59);
        npEndMinute.setValue(endMinute);
        npEndMinute.setFormatter(value -> String.format("%02d", value));

        btnCancel.setOnClickListener(v -> dismissWithSlideAnimation(timePickerDialog, dialogView));

        btnConfirm.setOnClickListener(v -> {
            int newStartHour = npStartHour.getValue();
            int newStartMinute = npStartMinute.getValue();
            int newEndHour = npEndHour.getValue();
            int newEndMinute = npEndMinute.getValue();

            long dayStart = DateUtils.getDayStartMillis(record.getStart());
            long newStart = dayStart + ((long) newStartHour * 60 + newStartMinute) * 60 * 1000;
            long newEnd = dayStart + ((long) newEndHour * 60 + newEndMinute) * 60 * 1000;

            if (newEnd <= newStart) {
                newEnd += 24L * 60 * 60 * 1000;
            }

            int newDurationMin = (int) ((newEnd - newStart) / (1000 * 60));

            record.setStart(newStart);
            record.setEnd(newEnd);
            record.setDuration(DateUtils.formatDuration(newDurationMin));
            record.setDurationMin(newDurationMin);

            listener.onRecordUpdated(position, record);
            dismissWithSlideAnimation(timePickerDialog, dialogView);
        });

        timePickerDialog.show();
    }

    public void showAddRecordDialog() {
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_add_record, null);
        DatePicker dpDate = dialogView.findViewById(R.id.dp_date);
        EditText etName = dialogView.findViewById(R.id.et_name);
        NumberPicker npStartHour = dialogView.findViewById(R.id.np_start_hour);
        NumberPicker npStartMinute = dialogView.findViewById(R.id.np_start_minute);
        NumberPicker npEndHour = dialogView.findViewById(R.id.np_end_hour);
        NumberPicker npEndMinute = dialogView.findViewById(R.id.np_end_minute);
        Button btnCancel = dialogView.findViewById(R.id.btn_cancel);
        Button btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        dpDate.setMaxDate(System.currentTimeMillis());

        npStartHour.setMinValue(0);
        npStartHour.setMaxValue(23);
        npStartHour.setFormatter(value -> String.format("%02d", value));

        npStartMinute.setMinValue(0);
        npStartMinute.setMaxValue(59);
        npStartMinute.setFormatter(value -> String.format("%02d", value));

        npEndHour.setMinValue(0);
        npEndHour.setMaxValue(23);
        npEndHour.setFormatter(value -> String.format("%02d", value));

        npEndMinute.setMinValue(0);
        npEndMinute.setMaxValue(59);
        npEndMinute.setFormatter(value -> String.format("%02d", value));

        Calendar now = Calendar.getInstance();
        npStartHour.setValue(now.get(Calendar.HOUR_OF_DAY));
        npStartMinute.setValue(now.get(Calendar.MINUTE));

        int defaultEndHour = now.get(Calendar.HOUR_OF_DAY) + 1;
        if (defaultEndHour >= 24) defaultEndHour = 23;
        npEndHour.setValue(defaultEndHour);
        npEndMinute.setValue(now.get(Calendar.MINUTE));

        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.TransparentDialogStyle);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getBehavior().setFitToContents(true);
        dialog.getBehavior().setSkipCollapsed(true);

        btnCancel.setOnClickListener(v -> dismissWithSlideAnimation(dialog, dialogView));

        btnConfirm.setOnClickListener(v -> {
            int year = dpDate.getYear();
            int month = dpDate.getMonth();
            int day = dpDate.getDayOfMonth();

            String name = etName.getText().toString().trim();
            if (name.isEmpty()) {
                name = "REC";
            }

            int startHour = npStartHour.getValue();
            int startMinute = npStartMinute.getValue();
            int endHour = npEndHour.getValue();
            int endMinute = npEndMinute.getValue();

            Calendar startCal = Calendar.getInstance();
            startCal.set(year, month, day, startHour, startMinute, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            Calendar endCal = Calendar.getInstance();
            endCal.set(year, month, day, endHour, endMinute, 0);
            endCal.set(Calendar.MILLISECOND, 0);

            long startTime = startCal.getTimeInMillis();
            long endTime = endCal.getTimeInMillis();

            if (endTime <= startTime) {
                endCal.add(Calendar.DAY_OF_MONTH, 1);
                endTime = endCal.getTimeInMillis();
            }

            int durationMin = (int) ((endTime - startTime) / 60000);

            java.util.List<TimerRecord> splitRecords = TimerRecordSplitter.splitRecord(name, startTime, endTime, durationMin);
            for (TimerRecord record : splitRecords) {
                dataManager.addRecord(record);
            }

            dismissWithSlideAnimation(dialog, dialogView);

            listener.onRecordAdded();
        });

        dialog.show();
    }

    private void dismissWithSlideAnimation(BottomSheetDialog dialog, View dialogView) {
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            dialogView.animate()
                    .translationY(dialogView.getHeight())
                    .alpha(0f)
                    .setDuration(400)
                    .setInterpolator(new OvershootInterpolator(-3f))
                    .withEndAction(dialog::dismiss)
                    .start();
        }, 400);
    }

    private long getTimeMillis(int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }
}
