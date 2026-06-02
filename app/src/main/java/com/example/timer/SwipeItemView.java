package com.example.timer;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;

public class SwipeItemView extends FrameLayout {

    private View deleteBtn;
    private View contentView;
    private float deleteBtnWidth;
    private float rawX;
    private float rawY;
    private float startX;
    private float startY;
    private boolean isSwipeOpen = false;
    private boolean isSwipeGesture = false;
    private boolean isHorizontalGesture = false;
    private OnDeleteClickListener deleteClickListener;
    private OnSwipeStateChangeListener swipeStateChangeListener;

    public interface OnDeleteClickListener {
        void onDeleteClick();
    }

    public interface OnSwipeStateChangeListener {
        void onSwipeOpened(SwipeItemView item);
        void onSwipeClosed(SwipeItemView item);
    }

    public SwipeItemView(Context context) {
        super(context);
        init();
    }

    public SwipeItemView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(true);
        setFocusable(true);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        deleteBtn = findViewById(R.id.delete_btn);
        contentView = findViewById(R.id.content_view);

        deleteBtnWidth = dpToPx(80);
    }

    private float dpToPx(float dp) {
        return dp * getContext().getResources().getDisplayMetrics().density;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> {
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick();
                }
            });
        }
    }

    public void setOnSwipeStateChangeListener(OnSwipeStateChangeListener listener) {
        this.swipeStateChangeListener = listener;
    }

    public void closeSwipe() {
        if (isSwipeOpen) {
            animateToX(0);
            isSwipeOpen = false;
            if (swipeStateChangeListener != null) {
                swipeStateChangeListener.onSwipeClosed(this);
            }
        }
    }

    private void animateToX(float targetX) {
        ValueAnimator animator = ValueAnimator.ofFloat(contentView.getTranslationX(), targetX);
        animator.setDuration(250);
        animator.setInterpolator(new OvershootInterpolator(1.0f));
        animator.addUpdateListener(animation -> {
            float x = (float) animation.getAnimatedValue();
            contentView.setTranslationX(x);
        });
        animator.start();
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                rawX = ev.getRawX();
                rawY = ev.getRawY();
                startX = ev.getRawX();
                startY = ev.getRawY();
                isSwipeGesture = false;
                isHorizontalGesture = false;
                requestDisallowParentIntercept(true);
                return false;

            case MotionEvent.ACTION_MOVE:
                float currentX = ev.getRawX();
                float currentY = ev.getRawY();
                float deltaX = currentX - rawX;
                float deltaY = currentY - rawY;
                float totalDeltaX = currentX - startX;
                float totalDeltaY = currentY - startY;

                if (!isHorizontalGesture && !isSwipeGesture) {
                    if (Math.abs(totalDeltaX) > 8 && Math.abs(totalDeltaX) > Math.abs(totalDeltaY) * 1.5f) {
                        isHorizontalGesture = true;
                        requestDisallowParentIntercept(true);
                    } else if (Math.abs(totalDeltaY) > 8 && Math.abs(totalDeltaY) > Math.abs(totalDeltaX) * 1.5f) {
                        requestDisallowParentIntercept(false);
                        return false;
                    }
                }

                if (isHorizontalGesture && Math.abs(deltaX) > Math.abs(deltaY) * 2 && Math.abs(deltaX) > 5) {
                    isSwipeGesture = true;
                    return true;
                }
                return false;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                requestDisallowParentIntercept(false);
                break;

            default:
                return false;
        }
        return false;
    }

    private void requestDisallowParentIntercept(boolean disallow) {
        ViewParent parent = getParent();
        while (parent != null) {
            parent.requestDisallowInterceptTouchEvent(disallow);
            parent = parent.getParent();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float clickX = event.getX();

        if (event.getAction() == MotionEvent.ACTION_UP) {
            if (isSwipeOpen && clickX > getWidth() + contentView.getTranslationX() - 10) {
                if (deleteBtn != null) {
                    animateButtonPress(deleteBtn);
                }
                if (deleteClickListener != null) {
                    deleteClickListener.onDeleteClick();
                    animateToX(0);
                    isSwipeOpen = false;
                    if (swipeStateChangeListener != null) {
                        swipeStateChangeListener.onSwipeClosed(this);
                    }
                    return true;
                }
            }
        }

        if (!isSwipeGesture && !isHorizontalGesture) {
            return super.onTouchEvent(event);
        }

        getParent().requestDisallowInterceptTouchEvent(true);

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                rawX = event.getRawX();
                rawY = event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                float currentX = event.getRawX();
                float currentY = event.getRawY();
                float deltaX = currentX - rawX;
                float deltaY = currentY - rawY;

                if (Math.abs(deltaX) > Math.abs(deltaY) * 1.5) {
                    rawX = currentX;
                    float currentTranslationX = contentView.getTranslationX();
                    float newTranslationX = currentTranslationX + deltaX;

                    float overshoot = 0;
                    if (newTranslationX < -deleteBtnWidth) {
                        overshoot = -deleteBtnWidth - newTranslationX;
                        newTranslationX = -deleteBtnWidth - overshoot * 0.3f;
                    } else if (newTranslationX > 0) {
                        overshoot = newTranslationX;
                        newTranslationX = overshoot * 0.3f;
                    }

                    contentView.setTranslationX(newTranslationX);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                float finalX = contentView.getTranslationX();

                if (finalX < -deleteBtnWidth / 2) {
                    animateToX(-deleteBtnWidth);
                    isSwipeOpen = true;
                    if (swipeStateChangeListener != null) {
                        swipeStateChangeListener.onSwipeOpened(this);
                    }
                } else {
                    animateToX(0);
                    isSwipeOpen = false;
                }
                isSwipeGesture = false;
                isHorizontalGesture = false;
                break;
        }
        return true;
    }

    private void animateButtonPress(View view) {
        view.setScaleX(0.9f);
        view.setScaleY(0.9f);
        
        ValueAnimator animator = ValueAnimator.ofFloat(0.9f, 1.0f);
        animator.setDuration(150);
        animator.addUpdateListener(animation -> {
            float scale = (float) animation.getAnimatedValue();
            view.setScaleX(scale);
            view.setScaleY(scale);
        });
        animator.start();
    }
}
