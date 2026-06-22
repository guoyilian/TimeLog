package com.example.timer;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
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
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.app.Dialog;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class TimerFragment extends Fragment {
    private TextView timerHours, timerMinutes, timerSeconds;
    private LinearLayout timerName;
    private TextView timerNameText;
    private LinearLayout btnPlayPause, btnStop, btnReset;
    private ImageView imagePlayPause, imageStop, imageReset;

    private List<TextView> tagViews;
    private LinearLayout cardContainer;

    private String currentTimerName = "";

    private Handler handler;
    private Runnable runnable;
    private boolean isRunning = false;
    private long startTime = 0;
    private long elapsedTime = 0;
    private boolean isPaused = false;
    private long firstStartTime = 0;  // 记录用户第一次点击开始计时的真实时间点

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

        timerHours = view.findViewById(R.id.timer_hours);
        timerMinutes = view.findViewById(R.id.timer_minutes);
        timerSeconds = view.findViewById(R.id.timer_seconds);

        timerName = view.findViewById(R.id.timer_name);
        timerNameText = view.findViewById(R.id.timer_name_text);
        cardContainer = view.findViewById(R.id.card_container);
        btnPlayPause = view.findViewById(R.id.btn_play_pause);
        btnStop = view.findViewById(R.id.btn_stop);
        btnReset = view.findViewById(R.id.btn_reset);
        imagePlayPause = view.findViewById(R.id.image_play_pause);
        imageStop = view.findViewById(R.id.image_stop);
        imageReset = view.findViewById(R.id.image_reset);

        tagViews = new ArrayList<>();
        tagViews.add(view.findViewById(R.id.tag1));
        tagViews.add(view.findViewById(R.id.tag2));
        tagViews.add(view.findViewById(R.id.tag3));
        tagViews.add(view.findViewById(R.id.tag4));

        dataManager = new DataManager(requireContext());

        imagePlayPause.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);
        imageStop.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);

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
        List<String> tags = TagRecommender.getRecommendedTags(records);
        TagRecommender.applyToViews(tagViews, tags, tag -> {
            currentTimerName = tag;
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
        // 使用 SystemClock.elapsedRealtime() 作为计时基准，它是单调递增的，不受系统时间修改影响
        startTime = SystemClock.elapsedRealtime() - elapsedTime;
        // 只有在第一次开始计时时才记录真实开始时间（用于保存记录）
        if (firstStartTime == 0) {
            firstStartTime = System.currentTimeMillis();
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && timerHours != null) {
                    // 使用单调时钟计算流逝时间
                    elapsedTime = SystemClock.elapsedRealtime() - startTime;
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
        imagePlayPause.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);
    }

    private void resumeTimer() {
        isRunning = true;
        isPaused = false;
        // 使用 SystemClock.elapsedRealtime() 作为计时基准
        startTime = SystemClock.elapsedRealtime() - elapsedTime;

        runnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning && timerHours != null) {
                    // 使用单调时钟计算流逝时间
                    elapsedTime = SystemClock.elapsedRealtime() - startTime;
                    updateTimerDisplay();
                    handler.postDelayed(this, 50);
                }
            }
        };
        handler.post(runnable);

        imagePlayPause.setImageResource(R.drawable.ic_pause);
        imagePlayPause.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);
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

            // 直接使用 long 时间戳：firstStartTime 是用户第一次点击开始计时的真实时间
            long endTime = System.currentTimeMillis();
            TimerRecord record = new TimerRecord(finalName, firstStartTime, endTime, totalMinutes + "分钟", totalMinutes);
            dataManager.addRecord(record);

            isRunning = false;
            isPaused = false;
            elapsedTime = 0;
            firstStartTime = 0;  // 重置首次开始时间
            updateTimerDisplay();
            currentTimerName = "";
            updateNameButtonAppearance();

            animateRunningToStart();
            hideResetButton();

            updateFrequentTags();

            if (listener != null) {
                listener.onRecordAdded();
            }

            if (dataManager != null) {
                dataManager.clearRunningTimerState();
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
        firstStartTime = 0;  // 重置首次开始时间
        updateTimerDisplay();
        currentTimerName = "";
        updateNameButtonAppearance();

        hideResetButton();
        animateRunningToStart();

        if (dataManager != null) {
            dataManager.clearRunningTimerState();
        }
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
                imagePlayPause.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);
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
                imagePlayPause.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);
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
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        timerHours.setText(String.format(Locale.getDefault(), "%02d", hours));
        timerMinutes.setText(String.format(Locale.getDefault(), "%02d", minutes));
        timerSeconds.setText(String.format(Locale.getDefault(), "%02d", seconds));
    }

    @Override
    public void onPause() {
        super.onPause();
        persistRunningState();
    }

    @Override
    public void onStop() {
        super.onStop();
        persistRunningState();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong("firstStartTime", firstStartTime);
        outState.putLong("elapsedTime", elapsedTime);
        outState.putLong("savedElapsedRealtime", SystemClock.elapsedRealtime());
        outState.putBoolean("isRunning", isRunning);
        outState.putBoolean("isPaused", isPaused);
        outState.putString("currentTimerName", currentTimerName);
    }

    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);
        if (savedInstanceState != null) {
            firstStartTime = savedInstanceState.getLong("firstStartTime", 0);
            long savedElapsedTime = savedInstanceState.getLong("elapsedTime", 0);
            long savedElapsedRealtime = savedInstanceState.getLong("savedElapsedRealtime", 0);
            isRunning = savedInstanceState.getBoolean("isRunning", false);
            isPaused = savedInstanceState.getBoolean("isPaused", false);
            currentTimerName = savedInstanceState.getString("currentTimerName", "");
            updateNameButtonAppearance();

            elapsedTime = savedElapsedTime;

            if (isRunning) {
                long currentElapsedRealtime = SystemClock.elapsedRealtime();

                // 优先使用单调时钟
                if (savedElapsedRealtime > 0 && currentElapsedRealtime > savedElapsedRealtime) {
                    long baseElapsed = savedElapsedRealtime - savedElapsedTime;
                    long calculatedElapsed = currentElapsedRealtime - baseElapsed;
                    elapsedTime = Math.max(savedElapsedTime, calculatedElapsed);
                }

                // 绝对安全检查
                if (elapsedTime < savedElapsedTime) {
                    elapsedTime = savedElapsedTime;
                }
                if (elapsedTime < 0) {
                    elapsedTime = 0;
                }

                startTime = currentElapsedRealtime - elapsedTime;
                updateTimerDisplay();

                runnable = new Runnable() {
                    @Override
                    public void run() {
                        if (isRunning && timerHours != null) {
                            elapsedTime = SystemClock.elapsedRealtime() - startTime;
                            updateTimerDisplay();
                            handler.postDelayed(this, 50);
                        }
                    }
                };
                handler.post(runnable);
                applyRunningButtonState(true);
                showResetButton();
            } else if (isPaused) {
                updateTimerDisplay();
                applyRunningButtonState(false);
                showResetButton();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        restoreRunningState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        // 在销毁时也保存状态，防止进程被强制杀死时状态丢失
        persistRunningState();
    }

    private void persistRunningState() {
        if (dataManager == null) return;

        if (isRunning || isPaused) {
            DataManager.RunningTimerState state = new DataManager.RunningTimerState(
                    firstStartTime,
                    elapsedTime,
                    System.currentTimeMillis(),
                    SystemClock.elapsedRealtime(),
                    isRunning,
                    isPaused,
                    currentTimerName
            );
            dataManager.saveRunningTimerState(state);
        } else {
            dataManager.clearRunningTimerState();
        }
    }

    private void applyRunningButtonState(boolean isRunning) {
        btnPlayPause.post(() -> {
            if (buttonOffset == 0) {
                int buttonWidth = btnPlayPause.getWidth();
                if (buttonWidth > 0) {
                    buttonOffset = (float) (buttonWidth / 2.0 + BUTTON_GAP / 2.0);
                }
            }

            if (isRunning) {
                imagePlayPause.setImageResource(R.drawable.ic_pause);
                imagePlayPause.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);
            } else {
                imagePlayPause.setImageResource(R.drawable.ic_play);
                imagePlayPause.setColorFilter(getResources().getColor(R.color.white, null), PorterDuff.Mode.SRC_IN);
            }

            btnPlayPause.setTranslationX(-buttonOffset);

            btnStop.setAlpha(1f);
            btnStop.setTranslationX(buttonOffset);
            btnStop.setVisibility(View.VISIBLE);

            btnReset.setAlpha(1f);
            btnReset.setScaleX(1f);
            btnReset.setScaleY(1f);
            btnReset.setVisibility(View.VISIBLE);
        });
    }

    private void restoreRunningState() {
        if (dataManager == null) return;
        if (isRunning || isPaused) return;

        DataManager.RunningTimerState state = dataManager.getRunningTimerState();
        if (state == null) return;

        firstStartTime = state.firstStartTime;
        elapsedTime = state.elapsedTime;
        currentTimerName = state.timerName != null ? state.timerName : "";
        updateNameButtonAppearance();

        if (state.isRunning) {
            isRunning = true;
            isPaused = false;

            long currentElapsedRealtime = SystemClock.elapsedRealtime();
            long savedElapsedTime = state.elapsedTime;

            // 策略1：优先使用单调时钟（最准确，不受系统时间修改影响）
            if (state.lastUpdateElapsedRealtime > 0 && currentElapsedRealtime > state.lastUpdateElapsedRealtime) {
                // 设备未重启，单调时钟有效
                // 保存时的基准时间 = lastUpdateElapsedRealtime - savedElapsedTime
                // 现在的流逝时间 = currentElapsedRealtime - 基准时间
                long baseElapsed = state.lastUpdateElapsedRealtime - savedElapsedTime;
                long calculatedElapsed = currentElapsedRealtime - baseElapsed;
                // 取较大值，确保不会因为任何原因变小
                elapsedTime = Math.max(savedElapsedTime, calculatedElapsed);
            } else {
                // 策略2：单调时钟被重置（设备重启）或无效
                long currentTime = System.currentTimeMillis();

                if (state.lastUpdateTime > 0) {
                    // 用真实时间计算从上次更新到现在的流逝
                    long realTimeGap = currentTime - state.lastUpdateTime;
                    if (realTimeGap > 0) {
                        // 如果真实时间差是正数，累加到保存的 elapsedTime 上
                        // 注意：如果用户在重启前暂停过，这个值可能偏大，但比丢失时间好
                        elapsedTime = savedElapsedTime + realTimeGap;
                    }
                }

                // 策略3：使用 firstStartTime 作为兜底（确保不会比整体时间差小）
                if (state.firstStartTime > 0) {
                    long totalElapsed = currentTime - state.firstStartTime;
                    // 取较大值，防止时间倒走
                    elapsedTime = Math.max(elapsedTime, totalElapsed);
                }

                // 最终兜底：绝不能小于保存的值
                if (elapsedTime < savedElapsedTime) {
                    elapsedTime = savedElapsedTime;
                }
            }

            // 绝对安全检查：绝不能为负数
            if (elapsedTime < 0) {
                elapsedTime = 0;
            }

            // 使用单调时钟设置新的基准时间
            startTime = currentElapsedRealtime - elapsedTime;
            updateTimerDisplay();

            runnable = new Runnable() {
                @Override
                public void run() {
                    if (isRunning && timerHours != null) {
                        elapsedTime = SystemClock.elapsedRealtime() - startTime;
                        updateTimerDisplay();
                        handler.postDelayed(this, 50);
                    }
                }
            };
            handler.post(runnable);

            applyRunningButtonState(true);
            showResetButton();
        } else if (state.isPaused) {
            isRunning = false;
            isPaused = true;
            updateTimerDisplay();

            applyRunningButtonState(false);
            showResetButton();
        }
    }
}
