package com.lora.cn.ui.view;

import android.content.Context;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;
import androidx.core.content.ContextCompat;
import com.lora.cn.R;

public class SignalStrengthView extends View {

    private int signalColor = Color.parseColor("#3AFCB8");
    private int emptyColor = Color.parseColor("#D8D8D8");
    private int barCount = 4;
    private int signalStrength = 4;

    public SignalStrengthView(Context context) {
        super(context);
        init(context);
    }

    public SignalStrengthView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public SignalStrengthView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        applyStrengthDrawable();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) dpToPx(getContext(), 24);
        int desiredHeight = (int) dpToPx(getContext(), 24);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int width, height;

        if (widthMode == MeasureSpec.EXACTLY) {
            width = widthSize;
        } else if (widthMode == MeasureSpec.AT_MOST) {
            width = Math.min(desiredWidth, widthSize);
        } else {
            width = desiredWidth;
        }

        if (heightMode == MeasureSpec.EXACTLY) {
            height = heightSize;
        } else if (heightMode == MeasureSpec.AT_MOST) {
            height = Math.min(desiredHeight, heightSize);
        } else {
            height = desiredHeight;
        }

        setMeasuredDimension(width, height);
    }

    /**
     * 设置信号强度
     * @param strength 信号强度 (0-4)
     */
    public void setSignalStrength(int strength) {
        signalStrength = Math.max(0, Math.min(strength, barCount));
        applyStrengthDrawable();
    }

    /**
     * 设置信号条颜色
     */
    public void setSignalColor(int color) {
        signalColor = color;
        invalidate();
    }

    /**
     * 设置无信号条颜色
     */
    public void setEmptyColor(int color) {
        emptyColor = color;
        invalidate();
    }

    private float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }

    private void applyStrengthDrawable() {
        int resId = getDrawableResForStrength(signalStrength);
        if (resId != 0) {
            setBackground(ContextCompat.getDrawable(getContext(), resId));
        } else {
            setBackground(null);
        }
        invalidate();
    }

    private int getDrawableResForStrength(int strength) {
        switch (strength) {
            case 0: return R.drawable.ic_xh_signal_0;
            case 1: return R.drawable.ic_xh_signal_1;
            case 2: return R.drawable.ic_xh_signal_2;
            case 3: return R.drawable.ic_xh_signal_3;
            case 4: return R.drawable.ic_xh_signal_4;
            default: return R.drawable.ic_xh_signal_0;
        }
    }
}
