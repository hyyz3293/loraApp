package com.lora.cn.ui.view;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import com.lora.cn.R;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {
    
    private Paint piePaint;
    private Paint linePaint;
    private Paint textPaint;
    private Paint namePaint; // name字段的画笔
    private Paint valuePaint; // value字段的画笔
    private Paint dotPaint;
    private Paint backgroundPaint;
    private RectF pieRect;
    private List<PieData> pieDataList;
    private int centerX, centerY;
    private float radius = 80f; // 默认圆环半径
    private float backgroundRadius = 87.5f; // 默认底部圆半径
    private boolean showLines = true; // 控制是否显示线条的开关
    private float ringWidth = 30f; // 圆环宽度
    private int lineColor = Color.parseColor("#D8D8D8"); // 线条颜色
    private int backgroundColor = Color.parseColor("#F1F4F8"); // 背景颜色
    private float textSize = 24f; // 文字大小
    private int textColor = Color.BLACK; // 文字颜色
    private float dotRadius = 4f; // 小白点半径
    private float gapAngle = 5f; // 模块间隔角度
    
    public PieChartView(Context context) {
        super(context);
        init(context, null);
    }
    
    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }
    
    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }
    
    private void init(Context context, AttributeSet attrs) {
        // 读取XML属性
        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PieChartView);
            
            radius = typedArray.getDimension(R.styleable.PieChartView_radius, radius);
            backgroundRadius = typedArray.getDimension(R.styleable.PieChartView_backgroundRadius, backgroundRadius);
            showLines = typedArray.getBoolean(R.styleable.PieChartView_showLines, showLines);
            ringWidth = typedArray.getDimension(R.styleable.PieChartView_ringWidth, ringWidth);
            lineColor = typedArray.getColor(R.styleable.PieChartView_lineColor, lineColor);
            backgroundColor = typedArray.getColor(R.styleable.PieChartView_backgroundColor, backgroundColor);
            textSize = typedArray.getDimension(R.styleable.PieChartView_textSize, textSize);
            textColor = typedArray.getColor(R.styleable.PieChartView_textColor, textColor);
            dotRadius = typedArray.getDimension(R.styleable.PieChartView_dotRadius, dotRadius);
            gapAngle = typedArray.getFloat(R.styleable.PieChartView_gapAngle, gapAngle);
            
            typedArray.recycle();
        }
        
        piePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        piePaint.setStyle(Paint.Style.STROKE);
        piePaint.setStrokeWidth(ringWidth);
        
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f);
        linePaint.setColor(lineColor);
        
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.WHITE);
        
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(backgroundColor);
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        
        // 初始化name字段画笔 - #898989颜色
        namePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        namePaint.setTextSize(textSize);
        namePaint.setColor(Color.parseColor("#898989"));
        
        // 初始化value字段画笔 - #000000颜色
        valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        valuePaint.setTextSize(textSize);
        valuePaint.setColor(Color.parseColor("#000000"));
        
        pieRect = new RectF();
        pieDataList = new ArrayList<>();
    }
    
    /**
     * 设置底部圆半径
     * @param backgroundRadius 底部圆半径
     */
    public void setBackgroundRadius(float backgroundRadius) {
        this.backgroundRadius = backgroundRadius;
        invalidate();
    }
    
    /**
     * 设置圆环半径
     * @param radius 圆环半径
     */
    public void setRadius(float radius) {
        this.radius = radius;
        updatePieRect();
        invalidate();
    }
    
    /**
     * 同时设置底部圆和圆环半径
     * @param backgroundRadius 底部圆半径
     * @param radius 圆环半径
     */
    public void setRadiuses(float backgroundRadius, float radius) {
        this.backgroundRadius = backgroundRadius;
        this.radius = radius;
        updatePieRect();
        invalidate();
    }
    
    /**
     * 获取底部圆半径
     * @return 底部圆半径
     */
    public float getBackgroundRadius() {
        return backgroundRadius;
    }
    
    /**
     * 获取圆环半径
     * @return 圆环半径
     */
    public float getRadius() {
        return radius;
    }
    
    private void updatePieRect() {
        if (centerX != 0 && centerY != 0) {
            pieRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
        }
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        updatePieRect();
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制最外层背景圆形
        canvas.drawCircle(centerX, centerY, backgroundRadius, backgroundPaint);
        
        if (pieDataList.isEmpty()) {
            return;
        }
        
        float startAngle = -90f; // 从12点钟方向开始
        float totalAngle = 0f;
        
        // 计算总角度，为间隔预留空间
        float totalGapAngle = pieDataList.size() * gapAngle;
        float availableAngle = 360f - totalGapAngle;
        
        for (int i = 0; i < pieDataList.size(); i++) {
            PieData data = pieDataList.get(i);
            float sweepAngle = (data.percentage / 100f) * availableAngle;
            
            // 绘制圆环扇形
            piePaint.setColor(data.color);
            canvas.drawArc(pieRect, startAngle, sweepAngle, false, piePaint);
            
            // 计算每个图块的中间角度（用于放置小白点）
            float midAngle = startAngle + sweepAngle / 2f;
            double radians = Math.toRadians(midAngle);
            
            // 小白点在圆环的中心点（圆环宽度的正中间）
            float dotRadiusPos = radius; // 圆环的中心线位置
            float dotX = centerX + (float) (dotRadiusPos * Math.cos(radians));
            float dotY = centerY + (float) (dotRadiusPos * Math.sin(radians));
            canvas.drawCircle(dotX, dotY, dotRadius, dotPaint);
            
            if (showLines) {
                // 引线从圆环外边缘开始
                float outerRadius = radius + ringWidth/2 + 15f; // 圆环外边缘
                float lineOuterStartX = centerX + (float) (outerRadius * Math.cos(radians));
                float lineOuterStartY = centerY + (float) (outerRadius * Math.sin(radians));
                
                // 引线中间点（在圆环外部）
                float lineMidX = centerX + (float) ((radius + 40) * Math.cos(radians));
                float lineMidY = centerY + (float) ((radius + 40) * Math.sin(radians));
                
                // 横向线段终点
                boolean isLeft = midAngle > 90 && midAngle < 270;
                float lineEndX = lineMidX + (isLeft ? -60 : 60);
                float lineEndY = lineMidY;
                
                // 绘制折线：从圆环外侧到中间点，再到横向终点
                canvas.drawLine(lineOuterStartX, lineOuterStartY, lineMidX, lineMidY, linePaint);
                canvas.drawLine(lineMidX, lineMidY, lineEndX, lineEndY, linePaint);
                
                // 分别绘制name和value字段，使用不同颜色
                String nameText = data.name;
                String valueText = data.value;
                
                float textCenterX = (lineMidX + lineEndX) / 2f; // 平行线中间位置
                
                // 计算文本宽度以便居中对齐
                float nameWidth = namePaint.measureText(nameText);
                float valueWidth = valuePaint.measureText(valueText);
                float totalWidth = nameWidth + valueWidth + 10f; // 10f为间距
                
                float startX = textCenterX - totalWidth / 2f;
                float textY = lineEndY - 5; // 文字在线条上方5dp
                
                // 绘制name字段（#898989颜色）
                canvas.drawText(nameText, startX, textY, namePaint);
                
                // 绘制value字段（#000000颜色）
                canvas.drawText(valueText, startX + nameWidth + 10f, textY, valuePaint);
            }
            
            // 下一个扇形的起始角度（加上间隔）
            startAngle += sweepAngle + gapAngle;
        }
    }
    
    public void setData(List<PieData> data) {
        this.pieDataList = data;
        invalidate();
    }
    
    public void setShowLines(boolean showLines) {
        this.showLines = showLines;
        invalidate();
    }
    
    public boolean isShowLines() {
        return showLines;
    }
    
    public static class PieData {
        public String name;  // 前面的字段
        public String value; // 后面的字段
        public float percentage;
        public int color;
        
        public PieData(String name, String value, float percentage, int color) {
            this.name = name;
            this.value = value;
            this.percentage = percentage;
            this.color = color;
        }
        
        // 为了向后兼容，保留原来的构造函数
        @Deprecated
        public PieData(String label, float percentage, int color) {
            this.name = label;
            this.value = percentage + "%";
            this.percentage = percentage;
            this.color = color;
        }
    }
    
    // 添加更多的setter方法来支持动态修改属性
    public void setRingWidth(float ringWidth) {
        this.ringWidth = ringWidth;
        piePaint.setStrokeWidth(ringWidth);
        invalidate();
    }
    
    public void setLineColor(int lineColor) {
        this.lineColor = lineColor;
        linePaint.setColor(lineColor);
        invalidate();
    }
    
    public void setBackgroundColor(int backgroundColor) {
        this.backgroundColor = backgroundColor;
        backgroundPaint.setColor(backgroundColor);
        invalidate();
    }
    
    public void setTextSize(float textSize) {
        this.textSize = textSize;
        textPaint.setTextSize(textSize);
        namePaint.setTextSize(textSize);
        valuePaint.setTextSize(textSize);
        invalidate();
    }
    
    public void setTextColor(int textColor) {
        this.textColor = textColor;
        textPaint.setColor(textColor);
        invalidate();
    }
    
    public void setDotRadius(float dotRadius) {
        this.dotRadius = dotRadius;
        invalidate();
    }
    
    public void setGapAngle(float gapAngle) {
        this.gapAngle = gapAngle;
        invalidate();
    }
    
    // 设置name字段颜色
    public void setNameColor(int nameColor) {
        namePaint.setColor(nameColor);
        invalidate();
    }
    
    // 设置value字段颜色
    public void setValueColor(int valueColor) {
        valuePaint.setColor(valueColor);
        invalidate();
    }
}