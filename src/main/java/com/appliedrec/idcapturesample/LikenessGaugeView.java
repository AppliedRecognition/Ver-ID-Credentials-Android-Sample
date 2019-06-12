package com.appliedrec.idcapturesample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;

/**
 * View that shows a gauge with a needle indicating a score between 0 and 1
 */

public class LikenessGaugeView extends View {

    private float score;
    private Paint needlePaint;
    private Paint needleDotPaint;
    private float threshold = 0.5f;
    private float max = 1.0f;
    private Paint passPaint;
    private Paint failPaint;
    private float strokeWidth = 20f;
    private RectF ovalRect;
    private float needleDotRadius = 12f;

    public LikenessGaugeView(Context context) {
        this(context, null, 0);
    }

    public LikenessGaugeView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public LikenessGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        score = 0f;
        float density = getContext().getResources().getDisplayMetrics().density;
        strokeWidth *=  density;
        needleDotRadius *= density;
        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setColor(Color.BLACK);
        needlePaint.setStrokeWidth(3 * density);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        needleDotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needleDotPaint.setStyle(Paint.Style.FILL);
        needleDotPaint.setColor(Color.BLACK);
        passPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        passPaint.setStyle(Paint.Style.STROKE);
        passPaint.setColor(getResources().getColor(R.color.verid_green));
        passPaint.setStrokeWidth(strokeWidth);
        passPaint.setStrokeCap(Paint.Cap.BUTT);
        failPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        failPaint.setStyle(Paint.Style.STROKE);
        failPaint.setColor(Color.RED);
        failPaint.setStrokeWidth(strokeWidth);
        failPaint.setStrokeCap(Paint.Cap.BUTT);
        ovalRect = new RectF(strokeWidth / 2f, strokeWidth / 2f, strokeWidth / 2f, strokeWidth / 2f);
    }

    @UiThread
    public void setScore(float score) {
        this.score = score;
        postInvalidate();
    }

    public float getScore() {
        return score;
    }

    public float getThreshold() {
        return threshold;
    }

    public void setThreshold(float threshold) {
        this.threshold = threshold;
        postInvalidate();
    }

    public float getMax() {
        return max;
    }

    public void setMax(float max) {
        this.max = max;
        postInvalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        float sweep = threshold / max * 180f;
        ovalRect.right = getWidth() - strokeWidth;
        ovalRect.bottom = getHeight() * 2 - strokeWidth;

        canvas.drawArc(ovalRect, 180f, sweep, false, failPaint);
        canvas.drawArc(ovalRect, 180f + sweep, 180f - sweep, false, passPaint);

        canvas.drawCircle(ovalRect.centerX(), getHeight(), needleDotRadius, needleDotPaint);

        double minAngle = Math.PI;
        double maxAngle = Math.PI * 2;
        double scoreAngle = score / max * (maxAngle - minAngle);

        canvas.drawLine(ovalRect.centerX(), getHeight(), (float)(ovalRect.centerX() - Math.cos(scoreAngle)*ovalRect.width()/2.0), (float)(getHeight() - Math.sin(scoreAngle)*ovalRect.height()), needlePaint);
    }
}
