package com.example.timer;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class FlowLayout extends ViewGroup {

    public FlowLayout(Context context) {
        super(context);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = widthMode == MeasureSpec.EXACTLY ? widthSize : 0;
        int height = getPaddingTop() + getPaddingBottom();
        int lineWidth = getPaddingLeft();
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();

            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childWidthWithMargins = childWidth + lp.leftMargin + lp.rightMargin;
            int childHeightWithMargins = childHeight + lp.topMargin + lp.bottomMargin;

            if (lineWidth + childWidthWithMargins > widthSize - getPaddingRight() && i > 0) {
                width = Math.max(width, lineWidth);
                height += lineHeight;
                lineWidth = getPaddingLeft();
                lineHeight = childHeightWithMargins;
            } else {
                lineHeight = Math.max(lineHeight, childHeightWithMargins);
            }
            lineWidth += childWidthWithMargins;
        }

        width = Math.max(width, lineWidth);
        height += lineHeight;

        setMeasuredDimension(
                widthMode == MeasureSpec.EXACTLY ? widthSize : width,
                heightMode == MeasureSpec.EXACTLY ? heightSize : height
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int width = r - l;
        int lineLeft = getPaddingLeft();
        int lineTop = getPaddingTop();
        int lineHeight = 0;

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            int childWidth = child.getMeasuredWidth();
            int childHeight = child.getMeasuredHeight();
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();
            int childWidthWithMargins = childWidth + lp.leftMargin + lp.rightMargin;
            int childHeightWithMargins = childHeight + lp.topMargin + lp.bottomMargin;

            if (lineLeft + childWidthWithMargins > width - getPaddingRight() && i > 0) {
                lineTop += lineHeight;
                lineLeft = getPaddingLeft();
                lineHeight = childHeightWithMargins;
            } else {
                lineHeight = Math.max(lineHeight, childHeightWithMargins);
            }

            int childLeft = lineLeft + lp.leftMargin;
            int childTop = lineTop + lp.topMargin;
            child.layout(childLeft, childTop, childLeft + childWidth, childTop + childHeight);
            lineLeft += childWidthWithMargins;
        }
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }
}
