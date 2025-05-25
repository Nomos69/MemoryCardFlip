package com.example.memorycardflip;

import android.content.Context;
import android.util.AttributeSet;

import androidx.appcompat.widget.AppCompatImageView;

public class SquareViewImage extends AppCompatImageView {
    public SquareViewImage(Context context) {
        super(context);
    }

    public SquareViewImage(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareViewImage(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Make height equal to width
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}