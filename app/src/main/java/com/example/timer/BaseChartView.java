package com.example.timer;

import android.content.Context;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.View.MeasureSpec;

public abstract class BaseChartView extends View {

    private float density;
    private float scaledDensity;

    public BaseChartView(Context context) {
        super(context);
        initDensity();
        init(null);
    }

    public BaseChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initDensity();
        init(attrs);
    }

    public BaseChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDensity();
        init(attrs);
    }

    private void initDensity() {
        density = getResources().getDisplayMetrics().density;
        scaledDensity = getResources().getDisplayMetrics().scaledDensity;
    }

    protected abstract void init(AttributeSet attrs);

    protected float dpToPx(float dp) {
        return dp * density;
    }

    protected int dpToPx(int dp) {
        return (int) (dp * density);
    }

    protected float spToPx(float sp) {
        return sp * scaledDensity;
    }

    protected int spToPx(int sp) {
        return (int) (sp * scaledDensity);
    }

    protected Paint createPaint() {
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        return paint;
    }

    protected Paint createPaint(int color, Paint.Style style) {
        Paint paint = createPaint();
        paint.setColor(color);
        paint.setStyle(style);
        return paint;
    }

    protected void measureDefaultHeight(int widthMeasureSpec, int heightMeasureSpec, int defaultHeightDp) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (heightMode == MeasureSpec.EXACTLY) {
            setMeasuredDimension(width, heightSize);
        } else {
            setMeasuredDimension(width, dpToPx(defaultHeightDp));
        }
    }
}
