package com.appliedrec.idcapturesample;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.util.AttributeSet;
import android.view.View;

/**
 * View that shows a gauge with a needle indicating a score between 0 and 1
 */

public class LikenessGaugeView extends View {

    private float score;
    private Paint needlePaint;
    private ValueAnimator scoreAnimator;

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
        setBackgroundResource(R.mipmap.similarity_dial);
        scoreAnimator = ValueAnimator.ofFloat(0f,0.4f);
        scoreAnimator.setDuration(8000);
        scoreAnimator.setRepeatMode(ValueAnimator.REVERSE);
        scoreAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                score = (float)animation.getAnimatedValue();
                postInvalidate();
            }
        });
        scoreAnimator.setRepeatCount(ValueAnimator.INFINITE);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Initially run an animation showing the needle slowly oscillate between 0 and 0.4
        scoreAnimator.start();
    }

    @UiThread
    public void setScore(final float score) {
        // When the score value is set animate the needle from the current value
        scoreAnimator.cancel();
        scoreAnimator.setFloatValues(this.score, score);
        scoreAnimator.setRepeatCount(0);
        scoreAnimator.setDuration(500);
        scoreAnimator.removeAllListeners();
        scoreAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {

            }

            @Override
            public void onAnimationEnd(Animator animation) {
                LikenessGaugeView.this.score = score;
                postInvalidate();
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                LikenessGaugeView.this.score = score;
                postInvalidate();
            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        scoreAnimator.start();
    }

    public float getScore() {
        return score;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float top = (float)getHeight() / 93f * 7f;
        RectF elipseRect = new RectF(0, top, getWidth(), top + (float)getWidth() / 248f * 158f);
        float angleOffset = 0.57f;
        float minAngle = (float)(Math.PI + angleOffset);
        float maxAngle = (float)(Math.PI * 2 - angleOffset);
        float angle = minAngle + score * (maxAngle - minAngle);
        float length = elipseRect.right - elipseRect.centerX();
        float height = (float)(Math.sin(angle) * length * (elipseRect.height() / elipseRect.width()));
        float width = (float)(Math.cos(angle) * length);
        PointF origin = new PointF(elipseRect.centerX(), elipseRect.centerY());
        PointF destination = new PointF(elipseRect.centerX() + width, elipseRect.centerY() + height);
        canvas.drawLine(origin.x, origin.y, destination.x, destination.y, needlePaint);
    }
}
