package com.appliedrec.credentials.app;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.MainThread;

public class DialView extends View {

    private int needleColor = Color.BLACK;
    private float needleWidth = 3f;
    private float ovalThickness = 30f;
    private final Path failPath = new Path();
    private final Path passPath = new Path();
    private final Path needlePath = new Path();
    private float failAngle = 90f;
    private float passAngle = 90f;
    private final Paint failPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint passPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needlePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint needleCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final PointF needleEnd = new PointF();
    private float needleCircleWidth = 8f;
    private double scoreAngle = 0;

    public DialView(Context context) {
        super(context);
        init(null, 0);
    }

    public DialView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(attrs, 0);
    }

    public DialView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(attrs, defStyle);
    }

    private void init(AttributeSet attrs, int defStyle) {
        // Load attributes
        final TypedArray a = getContext().obtainStyledAttributes(
                attrs, R.styleable.DialView, defStyle, 0);

        needleColor = a.getColor(R.styleable.DialView_needleColor, needleColor);

        float density = getResources().getDisplayMetrics().density;
        needleWidth = a.getDimension(R.styleable.DialView_needleWidth, needleWidth * density);

        ovalThickness = a.getDimension(R.styleable.DialView_ovalThickness, ovalThickness * density);

        a.recycle();

        needleCircleWidth = needleWidth * 3;

        failPaint.setColor(Color.RED);
        failPaint.setStyle(Paint.Style.STROKE);
        failPaint.setStrokeCap(Paint.Cap.BUTT);
        failPaint.setStrokeWidth(ovalThickness);

        passPaint.setColor(Color.GREEN);
        passPaint.setStyle(Paint.Style.STROKE);
        passPaint.setStrokeCap(Paint.Cap.BUTT);
        passPaint.setStrokeWidth(ovalThickness);

        needleCirclePaint.setColor(needleColor);
        needleCirclePaint.setStyle(Paint.Style.FILL);

        needlePaint.setColor(needleColor);
        needlePaint.setStyle(Paint.Style.STROKE);
        needlePaint.setStrokeCap(Paint.Cap.ROUND);
        needlePaint.setStrokeWidth(needleWidth);
    }

    @MainThread
    public void setScore(float score, float threshold, float max) {
        failAngle = threshold/max * 180;
        passAngle = 180 - failAngle;
        scoreAngle = score / max * Math.PI;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        needleEnd.x = (float)(getWidth()/2f - Math.cos(scoreAngle) * getWidth()/2f);
        needleEnd.y = (float)(getHeight() - Math.sin(scoreAngle) * getHeight());

        failPath.reset();
        failPath.addArc(ovalThickness/2f, ovalThickness/2f, getWidth()-ovalThickness/2f, getHeight()*2-ovalThickness/2f, 180f, failAngle);
        canvas.drawPath(failPath, failPaint);

        passPath.reset();
        passPath.addArc(ovalThickness/2f, ovalThickness/2f, getWidth()-ovalThickness/2f, getHeight()*2-ovalThickness/2f, 180f+failAngle, passAngle);
        canvas.drawPath(passPath, passPaint);

        needlePath.reset();
        needlePath.moveTo(getWidth()/2f, getHeight());
        needlePath.lineTo(needleEnd.x, needleEnd.y);
        canvas.drawPath(needlePath, needlePaint);

        canvas.drawOval(getWidth()/2f-needleCircleWidth/2f, getHeight()-needleCircleWidth/2f, getWidth()/2f+needleCircleWidth/2f, getHeight()+needleCircleWidth/2f, needleCirclePaint);
    }
}
