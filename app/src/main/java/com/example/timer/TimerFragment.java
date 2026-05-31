package com.example.timer;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.fragment.app.Fragment;
import android.app.Dialog;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TimerFragment extends Fragment {
    private TextView timerMinutes, timerSeconds, timerMilliseconds;
    private LinearLayout timerName;
    private TextView timerNameText;
    private LinearLayout btnPlayPause, btnStop, btnReset;
    private ImageView imagePlayPause, imageStop, imageReset;

    private TextView tag1, tag2, tag3, tag4;
    private LinearLayout cardContainer;

    private String currentTimerName = "";

    private Handler handler;
    private Runnable runnable;
    private boolean isRunning = false;
    private long startTime = 0;
    private long elapsedTime = 0;
    private boolean isPaused = false;

    private float buttonOffset = 0f;
    private static final int BUTTON_GAP = 200;

    private DataManager dataManager;

    public interface OnRecordAddedListener {
        void onRecordAdded();
    }

    private OnRecordAddedListener listener;

    public void setOnRecordAddedListener(OnRecordAddedListener listener) {
        this.listener = listener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        timerMinutes = view.findViewById(R.id.timer_minutes);
        timerSeconds = view.findViewById(R.id.timer_seconds);
        timerMilliseconds = view.findViewById(R.id.timer_milliseconds);

        timerName = view.findViewById(R.id.timer_name);
        timerNameText = view.findViewById(R.id.timer_name_text);
        cardContainer = view.findViewById(R.id.card_container);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnStop = view.findViewById(R.id.btn_stop);
        btnReset = view.findViewById(R.id.btn_reset);
        imagePlayPause = view.findViewById(R.id.image_play_pause);
        imageStop = view.findViewById(R.id.image_stop);
        imageReset = view.findViewById(R.id.image_reset);

        tag1 = view.findViewById(R.id.tag1);
        tag2 = view.findViewById(R.id.tag2);
        tag3 = view.findViewById(R.id.tag3);
        tag4 = view.findViewById(R.id.tag4);

        dataManager = new DataManager(requireContext());

        imagePlayPause.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
        imageStop.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);

        btnPlayPause.setOnClickListener(v -> togglePlayPause());
        btnStop.setOnClickListener(v -> stopTimer());
        btnReset.setOnClickListener(v -> resetTimer());

        handler = new Handler(Looper.getMainLooper());

        setupTimerNameEditText();
        updateFrequentTags();

        return view;
    }

    private void setupTimerNameEditText() {
        timerName.setOnClickListener(v -> showNameInputDialog());
    }

    private void updateNameButtonAppearance() {
        if (currentTimerName.isEmpty()) {
            timerNameText.setText("可选择输入自定义计时名称，默认为REC");
            timerNameText.setTextColor(getResources().getColor(R.color.input_hint, null));
            timerName.setBackground(getResources().getDrawable(R.drawable.input_field_bg, null));
        } else {
            timerNameText.setText(currentTimerName);
            timerNameText.setTextColor(getResources().getColor(R.color.tag_text, null));
            timerName.setBackground(null);
        }
    }

    private void showNameInputDialog() {
        View dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_input, null);
        EditText inputEditText = dialogView.findViewById(R.id.dialog_input);
        TextView btnCancel = dialogView.findViewById(R.id.btn_cancel);
        TextView btnConfirm = dialogView.findViewById(R.id.btn_confirm);

        inputEditText.setText(currentTimerName);
        inputEditText.setSelection(currentTimerName.length());
        inputEditText.requestFocus();

        inputEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm_active, null));
                    btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
                } else {
                    btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm, null));
                    btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        if (inputEditText.getText().toString().trim().isEmpty()) {
            btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm, null));
            btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
        } else {
            btnConfirm.setBackground(getResources().getDrawable(R.drawable.ripple_btn_confirm_active, null));
            btnConfirm.setTextColor(getResources().getColor(android.R.color.white, null));
        }

        androidx.appcompat.app.AlertDialog dialog = new androidx.appcompat.app.AlertDialog.Builder(requireContext(),
                R.style.TransparentDialogStyle)
                .setView(dialogView)
                .create();

        btnCancel.setOnClickListener(v -> {
            currentTimerName = inputEditText.getText().toString().trim();
            updateNameButtonAppearance();
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(200)
                .withEndAction(() -> dialog.dismiss())
                .start();
        });

        btnConfirm.setOnClickListener(v -> {
            currentTimerName = inputEditText.getText().toString().trim();
            updateNameButtonAppearance();
            v.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(200)
                .withEndAction(() -> dialog.dismiss())
                .start();
        });

        inputEditText.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE) {
                currentTimerName = inputEditText.getText().toString().trim();
                updateNameButtonAppearance();
                dialog.dismiss();
                return true;
            }
            return false;
        });

        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.getWindow().setDimAmount(0.4f);
        dialog.show();

        dialogView.setScaleX(0.8f);
        dialogView.setScaleY(0.8f);
        dialogView.setAlpha(0f);
        dialogView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1f)
            .setDuration(500)
            .setInterpolator(new OvershootInterpolator(1.2f))
            .start();

        dialog.setOnDismissListener(d -> {
            currentTimerName = inputEditText.getText().toString().trim();
            updateNameButtonAppearance();
        });
    }

    private void showCustomToast(boolean success) {
        if (getView() == null || cardContainer == null) return;

        View toastView = LayoutInflater.from(requireContext()).inflate(R.layout.custom_toast, null);
        TextView icon = toastView.findViewById(R.id.toast_icon);
        TextView text = toastView.findViewById(R.id.toast_text);

        if (success) {
            toastView.setBackground(getResources().getDrawable(R.drawable.toast_success_bg, null));
            icon.setBackground(getResources().getDrawable(R.drawable.ic_toast_success, null));
            icon.setText("✓");
            text.setText("已保存");
            text.setTextColor(getResources().getColor(R.color.toast_success_text, null));
        } else {
            toastView.setBackground(getResources().getDrawable(R.drawable.toast_error_bg, null));
            icon.setBackground(getResources().getDrawable(R.drawable.ic_toast_error, null));
            icon.setText("✕");
            text.setText("保存失败，计时不足30s");
            text.setTextColor(getResources().getColor(R.color.toast_error_text, null));
        }

        int[] location = new int[2];
        cardContainer.getLocationOnScreen(location);
        int cardTop = location[1];

        int toastHeight = (int)(40 * getResources().getDisplayMetrics().density);
        int gap = (int)(64 * getResources().getDisplayMetrics().density);
        int toastY = cardTop - toastHeight - gap;

        int toastWidth = success ? (int)(150 * getResources().getDisplayMetrics().density) : (int)(220 * getResources().getDisplayMetrics().density);
        
        Dialog dialog = new Dialog(requireContext(), R.style.ToastDialogStyle);
        dialog.setContentView(toastView);
        dialog.getWindow().setLayout(toastWidth, toastHeight);

        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.y = toastY;
        dialog.getWindow().setAttributes(params);

        toastView.setScaleX(0.8f);
        toastView.setScaleY(0.8f);
        toastView.setAlpha(0f);

        dialog.show();

        toastView.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1f)
            .setDuration(300)
            .setInterpolator(new OvershootInterpolator(1.5f))
            .start();

        Handler handler = new Handler(Looper.getMainLooper());
        handler.postDelayed(() -> {
            toastView.animate()
                .alpha(0f)
                .setDuration(300)
                .setInterpolator(new AccelerateDecelerateInterpolator())
                .withEndAction(dialog::dismiss)
                .start();
        }, 1000);
    }

    private void updateFrequentTags() {
        List<TimerRecord> records = dataManager.getRecords();
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
            if (entry.getValue() >= 5) {
                frequentNames.add(entry.getKey());
            }
        }

        String[] defaultTags = {"学习", "工作", "看书", "运动"};
        
        for (int i = 0; i < 4; i++) {
            String tagText;
            if (i < frequentNames.size()) {
                tagText = frequentNames.get(i);
            } else {
                int defaultIndex = i - frequentNames.size();
                String usedDefault = defaultTags[defaultIndex];
                while (frequentNames.contains(usedDefault) && defaultIndex + 1 < defaultTags.length) {
                    defaultIndex++;
                    usedDefault = defaultTags[defaultIndex];
                }
                tagText = usedDefault;
            }
            
            switch (i) {
                case 0:
                    tag1.setText(tagText);
                    tag1.setVisibility(View.VISIBLE);
                    setupTagClickListener(tag1);
                    break;
                case 1:
                    tag2.setText(tagText);
                    tag2.setVisibility(View.VISIBLE);
                    setupTagClickListener(tag2);
                    break;
                case 2:
                    tag3.setText(tagText);
                    tag3.setVisibility(View.VISIBLE);
                    setupTagClickListener(tag3);
                    break;
                case 3:
                    tag4.setText(tagText);
                    tag4.setVisibility(View.VISIBLE);
                    setupTagClickListener(tag4);
                    break;
            }
        }
    }

    private void updateTagVisibility(TextView tag, String name) {
        if (name != null && !name.isEmpty()) {
            tag.setText(name);
            tag.setVisibility(View.VISIBLE);
        } else {
            tag.setVisibility(View.GONE);
        }
    }

    private void setupTagClickListener(TextView tag) {
        tag.setOnClickListener(v -> {
            currentTimerName = tag.getText().toString();
            updateNameButtonAppearance();
        });
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        calculateButtonOffset();
    }

    private void calculateButtonOffset() {
        btnPlayPause.post(() -> {
            int buttonWidth = btnPlayPause.getWidth();
            buttonOffset = (float) (buttonWidth / 2.0 + BUTTON_GAP / 2.0);
        });
    }

    private void togglePlayPause() {
        if (!isRunning && !isPaused) {
            startTimer();
        } else if (isRunning) {
            pauseTimer();
        } else if (isPaused) {
            resumeTimer();
        }
    }

    private void startTimer() {
        isRunning = true;
        isPaused = false;
        startTime = System.currentTimeMillis() - elapsedTime;

        runnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && timerMinutes != null) {
                    elapsedTime = System.currentTimeMillis() - startTime;
                    updateTimerDisplay();
                    handler.postDelayed(this, 50);
                }
            }
        };
        handler.post(runnable);

        animateStartToRunning();
        showResetButton();
    }

    private void pauseTimer() {
        isRunning = false;
        isPaused = true;
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }

        imagePlayPause.setImageResource(R.drawable.ic_play);
        imagePlayPause.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
    }

    private void resumeTimer() {
        isRunning = true;
        isPaused = false;
        startTime = System.currentTimeMillis() - elapsedTime;

        runnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && timerMinutes != null) {
                    elapsedTime = System.currentTimeMillis() - startTime;
                    updateTimerDisplay();
                    handler.postDelayed(this, 50);
                }
            }
        };
        handler.post(runnable);

        imagePlayPause.setImageResource(R.drawable.ic_pause);
        imagePlayPause.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
    }

    private void stopTimer() {
        try {
            if (handler != null && runnable != null) {
                handler.removeCallbacks(runnable);
            }

            if (timerMinutes == null || timerName == null) {
                return;
            }

            long totalSeconds = elapsedTime / 1000;
            if (totalSeconds < 30) {
                showCustomToast(false);
                isRunning = false;
                isPaused = false;
                elapsedTime = 0;
                updateTimerDisplay();
                currentTimerName = "";
                updateNameButtonAppearance();
                animateRunningToStart();
                hideResetButton();
                return;
            }

            String finalName = currentTimerName.trim();
            if (finalName.isEmpty()) {
                finalName = "REC";
            }

            int totalMinutes = Math.max(1, (int) Math.round(elapsedTime / 60000f));

            Date now = new Date();
            SimpleDateFormat sdfDateTime = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
            String endStr = sdfDateTime.format(now);

            int startMinutes = (int) (now.getTime() / 60000) - totalMinutes;
            Date startDate = new Date(startMinutes * 60000L);
            String startStr = sdfDateTime.format(startDate);

            TimerRecord record = new TimerRecord(finalName, startStr, endStr, totalMinutes + "分钟", totalMinutes);
            dataManager.addRecord(record);

            isRunning = false;
            isPaused = false;
            elapsedTime = 0;
            updateTimerDisplay();
            currentTimerName = "";
            updateNameButtonAppearance();

            animateRunningToStart();
            hideResetButton();

            updateFrequentTags();

            if (listener != null) {
                listener.onRecordAdded();
            }

            showCustomToast(true);
        } catch (Exception e) {
            e.printStackTrace();
            showCustomToast(false);
        }
    }

    private void resetTimer() {
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }

        isRunning = false;
        isPaused = false;
        elapsedTime = 0;
        updateTimerDisplay();
        currentTimerName = "";
        updateNameButtonAppearance();

        hideResetButton();
        animateRunningToStart();
    }

    private void showResetButton() {
        btnReset.setAlpha(0f);
        btnReset.setScaleX(0.5f);
        btnReset.setScaleY(0.5f);
        btnReset.setVisibility(View.VISIBLE);

        ObjectAnimator alphaAnim = ObjectAnimator.ofFloat(btnReset, "alpha", 0f, 1f);
        ObjectAnimator scaleXAnim = ObjectAnimator.ofFloat(btnReset, "scaleX", 0.5f, 1f);
        ObjectAnimator scaleYAnim = ObjectAnimator.ofFloat(btnReset, "scaleY", 0.5f, 1f);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(alphaAnim, scaleXAnim, scaleYAnim);
        animatorSet.setDuration(200);
        animatorSet.start();
    }

    private void hideResetButton() {
        btnReset.setVisibility(View.GONE);
    }

    private void animateStartToRunning() {
        if (buttonOffset == 0) {
            btnPlayPause.post(this::calculateButtonOffset);
        }

        btnStop.setAlpha(0f);
        btnStop.setTranslationX(0f);
        btnStop.setVisibility(View.VISIBLE);

        ObjectAnimator playPauseAnim = ObjectAnimator.ofFloat(btnPlayPause, "translationX", 0f, -buttonOffset);
        ObjectAnimator stopAnim = ObjectAnimator.ofPropertyValuesHolder(btnStop,
                PropertyValuesHolder.ofFloat("translationX", 0f, buttonOffset),
                PropertyValuesHolder.ofFloat("alpha", 0f, 1f));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(playPauseAnim, stopAnim);
        animatorSet.setDuration(350);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());

        animatorSet.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                imagePlayPause.setImageResource(R.drawable.ic_pause);
                imagePlayPause.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {}

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });

        animatorSet.start();
    }

    private void animateRunningToStart() {
        ObjectAnimator playPauseAnim = ObjectAnimator.ofFloat(btnPlayPause, "translationX", -buttonOffset, 0f);
        ObjectAnimator stopAnim = ObjectAnimator.ofPropertyValuesHolder(btnStop,
                PropertyValuesHolder.ofFloat("translationX", buttonOffset, 0f),
                PropertyValuesHolder.ofFloat("alpha", 1f, 0f));

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(playPauseAnim, stopAnim);
        animatorSet.setDuration(350);
        animatorSet.setInterpolator(new AccelerateDecelerateInterpolator());

        animatorSet.addListener(new android.animation.Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(android.animation.Animator animation) {
                imagePlayPause.setImageResource(R.drawable.ic_play);
                imagePlayPause.setColorFilter(getResources().getColor(R.color.white), PorterDuff.Mode.SRC_IN);
            }

            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                btnStop.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onAnimationCancel(android.animation.Animator animation) {}

            @Override
            public void onAnimationRepeat(android.animation.Animator animation) {}
        });

        animatorSet.start();
    }

    private void updateTimerDisplay() {
        long totalSeconds = elapsedTime / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        long milliseconds = (elapsedTime % 1000) / 10;

        timerMinutes.setText(String.format(Locale.getDefault(), "%02d", minutes));
        timerSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
        timerMilliseconds.setText(String.format(Locale.getDefault(), "%02d", milliseconds));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
