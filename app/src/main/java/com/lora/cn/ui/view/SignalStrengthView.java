package com.lora.cn.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

public class SignalStrengthView extends View {

    private int signalColor = Color.parseColor("#3AFCB8");
    private int emptyColor = Color.parseColor("#D8D8D8");
    private int barCount = 4;
    private float barSpacing;
    private float barCornerRadius;
    private int signalStrength = 4;

    private Paint paint;
    private Path path;

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
        barSpacing = dpToPx(context, 4);
        barCornerRadius = dpToPx(context, 2);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setColor(signalColor);
        paint.setStyle(Paint.Style.FILL);

        path = new Path();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float totalWidth = getWidth();
        float totalHeight = getHeight();

        // 计算每个信号条的宽度
        float barWidth = (totalWidth - (barCount - 1) * barSpacing) / barCount;

        // 计算每个信号条的高度增量
        float heightIncrement = totalHeight / barCount;

        // 绘制每个信号条（无信号为灰色#D8D8D8，有信号为蓝色#5B8CFF）
        for (int i = 0; i < barCount; i++) {
            float left = i * (barWidth + barSpacing);
            float right = left + barWidth;

            // 当前信号条的高度 (从右到左递增)
            float barHeight = heightIncrement * (i + 1);

            // 当前信号条的顶部位置 (从底部开始)
            float top = totalHeight - barHeight;

            // 创建圆角矩形
            RectF rect = new RectF(left, top, right, totalHeight);

            // 按强度选择颜色绘制
            paint.setColor(i < signalStrength ? signalColor : emptyColor);
            canvas.drawRoundRect(rect, barCornerRadius, barCornerRadius, paint);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int desiredWidth = (int) dpToPx(getContext(), 60);
        int desiredHeight = (int) dpToPx(getContext(), 30);

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
        invalidate();
    }

    /**
     * 设置信号条颜色
     */
    public void setSignalColor(int color) {
        signalColor = color;
        paint.setColor(signalColor);
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
}