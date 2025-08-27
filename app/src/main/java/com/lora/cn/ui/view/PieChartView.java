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
        piePaint.setStrokeWidth(35f); // 圆环宽度35dp
        
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
        float backgroundRadius = 87.5f; // 175/2 = 87.5
        canvas.drawCircle(centerX, centerY, backgroundRadius, backgroundPaint);
        
        if (pieDataList.isEmpty()) {
            return;
        }
        
        float startAngle = -90f; // 从12点钟方向开始
        
        for (int i = 0; i < pieDataList.size(); i++) {
            PieData data = pieDataList.get(i);
            float sweepAngle = data.percentage * 360f / 100f;
            
            // 绘制圆环扇形
            piePaint.setColor(data.color);
            canvas.drawArc(pieRect, startAngle, sweepAngle, false, piePaint);
            
            // 计算引线起点和终点
            float midAngle = startAngle + sweepAngle / 2f;
            double radians = Math.toRadians(midAngle);
            
            if (showLines) {
                // 引线起点在圆环内边缘（圆环内侧）- 用于绘制白色小圆点
                float innerRadius = radius - 17.5f; // 圆环宽度35dp的一半
                float lineStartX = centerX + (float) (innerRadius * Math.cos(radians));
                float lineStartY = centerY + (float) (innerRadius * Math.sin(radians));
                
                // 引线从圆环外边缘开始
                float outerRadius = radius + 17.5f; // 圆环外边缘
                float lineOuterStartX = centerX + (float) (outerRadius * Math.cos(radians));
                float lineOuterStartY = centerY + (float) (outerRadius * Math.sin(radians));
                
                // 引线中间点（在圆环外部）
                float lineMidX = centerX + (float) ((radius + 40) * Math.cos(radians));
                float lineMidY = centerY + (float) ((radius + 40) * Math.sin(radians));
                
                // 横向线段终点
                boolean isLeft = midAngle > 90 && midAngle < 270; // 调整判断条件适应-90度起始角
                float lineEndX = lineMidX + (isLeft ? -60 : 60);
                float lineEndY = lineMidY;
                
                // 绘制折线：从圆环外侧到中间点，再到横向终点
                canvas.drawLine(lineOuterStartX, lineOuterStartY, lineMidX, lineMidY, linePaint);
                canvas.drawLine(lineMidX, lineMidY, lineEndX, lineEndY, linePaint);
                
                // 在线条头部（圆环内侧）绘制白色小圆点
                canvas.drawCircle(lineStartX, lineStartY, 4f, dotPaint);
                
                // 绘制文本在横向线段上方
                String text = data.label + " " + data.percentage + "%";
                float textX = isLeft ? lineEndX - textPaint.measureText(text) : lineEndX;
                float textY = lineEndY - 10; // 文字在线条上方10dp
                
                canvas.drawText(text, textX, textY, textPaint);
            }
            
            startAngle += sweepAngle;
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