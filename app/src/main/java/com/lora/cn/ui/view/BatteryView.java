package com.lora.cn.ui.view;

import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.view.View;

public class BatteryView extends View {

    // 电池颜色配置
    private int lowBatteryOuterColor = Color.parseColor("#F0C4C4");
    private int lowBatteryInnerColor = Color.parseColor("#DF1111");
    private int highBatteryOuterColor = Color.parseColor("#B9E8D8");
    private int highBatteryInnerColor = Color.parseColor("#3AFCB8");

    // 电池参数
    private int batteryLevel = 75; // 电量百分比 0-100
    private final int segmentCount = 4; // 内部方格数量
    private float cornerRadius;
    private float terminalWidth; // 正极宽度
    private float terminalHeight; // 正极高度
    private float terminalGap; // 正极与主体间距
    private float segmentSpacing; // 内部方格间距

    private Paint outerPaint;
    private Paint innerPaint;
    private Paint terminalPaint;
    private Paint emptySegmentPaint;

    public BatteryView(Context context) {
        super(context);
        init(context);
    }

    public BatteryView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public BatteryView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        cornerRadius = dpToPx(context, 2);
        terminalWidth = dpToPx(context, 2);
        terminalHeight = dpToPx(context, 3);
        terminalGap = dpToPx(context, 2);
        segmentSpacing = dpToPx(context, 3.5f);

        // 外层电池油漆（透明背景，仅边框）
        outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        outerPaint.setStyle(Paint.Style.STROKE);
        outerPaint.setStrokeWidth(dpToPx(context, 2f));

        // 内部电量油漆
        innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        innerPaint.setStyle(Paint.Style.FILL);

        // 正极油漆
        terminalPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        terminalPaint.setStyle(Paint.Style.FILL);

        // 空方格油漆（无电量方块颜色 #D3E1DC）
        emptySegmentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        emptySegmentPaint.setStyle(Paint.Style.FILL);
        emptySegmentPaint.setColor(Color.parseColor("#D3E1DC"));

        updateColors();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float width = getWidth();
        float height = getHeight();

        // 计算电池主体区域（不包括正极）
        float bodyWidth = width - terminalWidth - terminalGap;
        float bodyHeight = height;

        // 绘制电池外层主体（圆角矩形边框）
        RectF bodyRect = new RectF(0, 0, bodyWidth, bodyHeight);
        canvas.drawRoundRect(bodyRect, cornerRadius, cornerRadius, outerPaint);

        // 绘制电池正极（右侧小圆点）
        float terminalLeft = bodyWidth + terminalGap;
        float terminalTop = (height - terminalHeight) / 2;
        float terminalRight = width;
        float terminalBottom = terminalTop + terminalHeight;
        RectF terminalRect = new RectF(terminalLeft, terminalTop, terminalRight, terminalBottom);
        canvas.drawRoundRect(terminalRect, cornerRadius, cornerRadius, terminalPaint);

        // 绘制内部电量方格
        drawBatterySegments(canvas, bodyRect);
    }

    private void drawBatterySegments(Canvas canvas, RectF bodyRect) {
        float segmentWidth = (bodyRect.width() - (segmentCount + 1) * segmentSpacing) / segmentCount;
        float top = bodyRect.top + segmentSpacing;
        float bottom = bodyRect.bottom - segmentSpacing;
        float segmentHeight = bottom - top;

        for (int i = 0; i < segmentCount; i++) {
            int segmentThreshold = (i + 1) * (100 / segmentCount);
            float left = bodyRect.left + segmentSpacing + i * (segmentWidth + segmentSpacing);
            float right = left + segmentWidth;
            RectF segmentRect = new RectF(left, top, right, bottom);

            if (batteryLevel >= segmentThreshold) {
                canvas.drawRect(segmentRect, innerPaint);
            } else {
                canvas.drawRect(segmentRect, emptySegmentPaint);
            }
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
     * 设置电池电量
     * @param level 电量百分比 (0-100)
     */
    public void setBatteryLevel(int level) {
        this.batteryLevel = Math.max(0, Math.min(level, 100));
        updateColors();
        invalidate();
    }

    /**
     * 根据电量更新颜色
     */
    private void updateColors() {
        // 判断是否为低电量（这里假设低于20%为低电量）
        boolean isLowBattery = batteryLevel <= 20;

        if (isLowBattery) {
            outerPaint.setColor(lowBatteryOuterColor);
            innerPaint.setColor(lowBatteryInnerColor);
            terminalPaint.setColor(lowBatteryOuterColor);
        } else {
            outerPaint.setColor(highBatteryOuterColor);
            innerPaint.setColor(highBatteryInnerColor);
            terminalPaint.setColor(highBatteryOuterColor);
        }
    }

    /**
     * 设置低电量颜色
     */
    public void setLowBatteryColors(int outerColor, int innerColor) {
        this.lowBatteryOuterColor = outerColor;
        this.lowBatteryInnerColor = innerColor;
        updateColors();
        invalidate();
    }

    /**
     * 设置高电量颜色
     */
    public void setHighBatteryColors(int outerColor, int innerColor) {
        this.highBatteryOuterColor = outerColor;
        this.highBatteryInnerColor = innerColor;
        updateColors();
        invalidate();
    }

    /**
     * dp 转 px
     */
    private float dpToPx(Context context, float dp) {
        return dp * context.getResources().getDisplayMetrics().density;
    }
}
