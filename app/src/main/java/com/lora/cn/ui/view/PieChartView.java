package com.lora.cn.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class PieChartView extends View {
    
    private Paint piePaint;
    private Paint linePaint;
    private Paint textPaint;
    private Paint dotPaint;
    private Paint backgroundPaint;
    private RectF pieRect;
    private List<PieData> pieDataList;
    private int centerX, centerY;
    private int radius;
    private boolean showLines = true; // 控制是否显示线条的开关
    
    public PieChartView(Context context) {
        super(context);
        init();
    }
    
    public PieChartView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    
    public PieChartView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    
    private void init() {
        piePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        piePaint.setStyle(Paint.Style.STROKE);
        piePaint.setStrokeWidth(30f); // 圆环宽度30dp，为间隔留出空间
        
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f); // 线条宽度1dp
        linePaint.setColor(Color.parseColor("#D8D8D8")); // 线条颜色
        
        dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        dotPaint.setStyle(Paint.Style.FILL);
        dotPaint.setColor(Color.WHITE); // 白色小圆点
        
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setStyle(Paint.Style.FILL);
        backgroundPaint.setColor(Color.parseColor("#F1F4F8")); // 背景圆形颜色
        
        textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(24f);
        textPaint.setColor(Color.BLACK);
        
        pieRect = new RectF();
        pieDataList = new ArrayList<>();
    }
    
    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        centerX = w / 2;
        centerY = h / 2;
        radius = 80; // 直径160dp，半径80dp
        
        pieRect.set(centerX - radius, centerY - radius, centerX + radius, centerY + radius);
    }
    
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        
        // 绘制最外层背景圆形 175*175
        float backgroundRadius = 110f; // 175/2 = 87.5
        canvas.drawCircle(centerX, centerY, backgroundRadius, backgroundPaint);
        
        if (pieDataList.isEmpty()) {
            return;
        }
        
        float startAngle = -90f; // 从12点钟方向开始
        float totalAngle = 0f;
        
        // 计算总角度，为间隔预留空间
        float gapAngle = 5f; // 每个模块间隔5度
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
            float dotRadius = radius; // 圆环的中心线位置
            float dotX = centerX + (float) (dotRadius * Math.cos(radians));
            float dotY = centerY + (float) (dotRadius * Math.sin(radians));
            canvas.drawCircle(dotX, dotY, 4f, dotPaint);
            
            if (showLines) {
                // 引线从圆环外边缘开始
                float outerRadius = radius + 15f; // 圆环外边缘
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
                
                // 绘制文本在平行线中间（横向线段的中点）
                String text = data.label + " " + data.percentage + "%";
                float textCenterX = (lineMidX + lineEndX) / 2f; // 平行线中间位置
                float textX = isLeft ? textCenterX - textPaint.measureText(text) / 2f : textCenterX - textPaint.measureText(text) / 2f;
                float textY = lineEndY - 5; // 文字在线条上方5dp
                
                canvas.drawText(text, textX, textY, textPaint);
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
        public String label;
        public float percentage;
        public int color;
        
        public PieData(String label, float percentage, int color) {
            this.label = label;
            this.percentage = percentage;
            this.color = color;
        }
    }
}