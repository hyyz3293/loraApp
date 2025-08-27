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
    private RectF pieRect;
    private List<PieData> pieDataList;
    private int centerX, centerY;
    private int radius;
    
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
        piePaint.setStrokeWidth(35f); // 圆环宽度35dp
        
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setStyle(Paint.Style.STROKE);
        linePaint.setStrokeWidth(1f); // 线条宽度1dp
        linePaint.setColor(Color.parseColor("#D8D8D8")); // 线条颜色
        
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
        
        if (pieDataList.isEmpty()) {
            return;
        }
        
        float startAngle = 0f;
        
        for (int i = 0; i < pieDataList.size(); i++) {
            PieData data = pieDataList.get(i);
            float sweepAngle = data.percentage * 360f / 100f;
            
            // 绘制圆环扇形
            piePaint.setColor(data.color);
            canvas.drawArc(pieRect, startAngle, sweepAngle, false, piePaint);
            
            // 计算引线起点和终点
            float midAngle = startAngle + sweepAngle / 2f;
            double radians = Math.toRadians(midAngle);
            
            // 引线起点在圆环外边缘
            float lineStartX = centerX + (float) (radius * Math.cos(radians));
            float lineStartY = centerY + (float) (radius * Math.sin(radians));
            
            // 引线中间点
            float lineMidX = centerX + (float) (radius * 1.2 * Math.cos(radians));
            float lineMidY = centerY + (float) (radius * 1.2 * Math.sin(radians));
            
            // 横向线段终点
            boolean isLeft = midAngle > 90 && midAngle < 270;
            float lineEndX = lineMidX + (isLeft ? -60 : 60);
            float lineEndY = lineMidY;
            
            // 绘制折线：从圆环到中间点，再到横向终点
            canvas.drawLine(lineStartX, lineStartY, lineMidX, lineMidY, linePaint);
            canvas.drawLine(lineMidX, lineMidY, lineEndX, lineEndY, linePaint);
            
            // 绘制文本在横向线段上方
            String text = data.label + " " + data.percentage + "%";
            float textX = isLeft ? lineEndX - textPaint.measureText(text) : lineEndX;
            float textY = lineEndY - 10; // 文字在线条上方10dp
            
            canvas.drawText(text, textX, textY, textPaint);
            
            startAngle += sweepAngle;
        }
    }
    
    public void setData(List<PieData> data) {
        this.pieDataList = data;
        invalidate();
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