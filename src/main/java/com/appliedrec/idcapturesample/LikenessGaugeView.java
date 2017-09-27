package com.appliedrec.idcapturesample;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by jakub on 19/09/2017.
 */

public class LikenessGaugeView extends View {

    private float score;
    private Paint needlePaint;

    public LikenessGaugeView(Context context) {
        super(context);
        init();
    }

    public LikenessGaugeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public LikenessGaugeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        score = 0f;
        needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setColor(Color.BLACK);
        needlePaint.setStrokeWidth(3 * getContext().getResources().getDisplayMetrics().density);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        setBackgroundResource(R.mipmap.likeness_gauge);
    }

    @UiThread
    public void setScore(float score) {
        this.score = score;
        invalidate();
    }

    public float getScore() {
        return score;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        double maxScore = 1.0;
        double sweep = (double) score / maxScore * Math.PI;
        float x = (float)(Math.cos(sweep) * (double) getWidth() / 2.0);
        float y = (float)(Math.sin(sweep) * (double) getWidth() / 2.0);
        canvas.drawLine(getWidth() / 2f, getHeight(), getWidth() / 2f - x, getHeight() - y, needlePaint);
    }
}
