package com.autel.widget.widget.histogram;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class HistogramChartView extends View {
    private final static String TAG = HistogramChartView.class.getSimpleName();

    // 线条画笔
    private Paint linePaint;

    private int mViewWidth;
    private int mViewHeight;

    private float lineWidth;

    private float maxValue;

    private int[] mHistogramValues;

    private final Point p1 = new Point();
    private final Point p2 = new Point();
    private final Point p3 = new Point();

    private float firstMultiplier;
    private float secondMultiplier;

    private final ArrayList<Point> points = new ArrayList<>();

    private final AtomicBoolean valueLock = new AtomicBoolean(false);

    public HistogramChartView(Context context) {
        super(context);
        init(context);
    }

    public HistogramChartView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HistogramChartView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mViewWidth = getWidth();
        mViewHeight = getHeight();
    }

    private void init(Context context) {
        linePaint = new Paint();
        linePaint.setAntiAlias(true);
        linePaint.setStyle(Paint.Style.FILL);
        linePaint.setStrokeWidth(1);
        linePaint.setAlpha(178);
        linePaint.setColor(Color.WHITE);

        this.firstMultiplier = 0.33F;
        this.secondMultiplier = 1.0F - this.firstMultiplier;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mHistogramValues != null) {
            drawPath(canvas, points, linePaint);
        } else {
            super.onDraw(canvas);
        }

        valueLock.set(false);
    }

    private void drawPath(Canvas canvas, ArrayList<Point> points, Paint paint) {
        Path p = new Path();
        float x = points.get(0).getX();
        float y = points.get(0).getY();
        p.moveTo(x, y);
        int length = points.size();

        int i;
        for (i = 0; i < length; i++) {
            int nextIndex = i + 1 < length ? i + 1 : i;
            int nextNextIndex = i + 2 < length ? i + 2 : nextIndex;
            this.calc(points, this.p1, i, nextIndex, this.secondMultiplier);
            this.p2.setX((points.get(nextIndex).getX()));
            this.p2.setY((points.get(nextIndex).getY()));
            this.calc(points, this.p3, nextIndex, nextNextIndex, this.firstMultiplier);
            p.cubicTo(this.p1.getX(), this.p1.getY(), this.p2.getX(), this.p2.getY(), this.p3.getX(), this.p3.getY());
        }

        canvas.drawPath(p, paint);
    }

    private void calc(ArrayList<Point> points, Point result, int index1, int index2, float multiplier) {
        Point point1 = points.get(index1);
        float p1x = 0f;
        float p1y = 0f;
        if (point1 != null) {
            p1x = point1.getX();
            p1y = point1.getY();
        }

        Point point2 = points.get(index2);
        float p2x = 0f;
        float p2y = 0f;
        if (point2 != null) {
            p2x = point2.getX();
            p2y = point2.getY();
        }
        float diffX = p2x - p1x;
        float diffY = p2y - p1y;
        result.setX(p1x + diffX * multiplier);
        result.setY(p1y + diffY * multiplier);
    }

    public void updateHistogramValues(int[] values) {
        if (valueLock.get() || values == null || values.length == 0) {
            return;
        }
        long start = System.currentTimeMillis();
        valueLock.set(true);
        this.points.clear();

        Point pointStart = new Point();
        pointStart.setX(0);
        pointStart.setY(mViewHeight);
        this.points.add(pointStart);
        int size = values.length;
        // 灰度值分布
        int[] grayscales = new int[256];
        for (int i = 0; i < size; i++) {
            int v = values[i];
            if (v != 0) {
                grayscales[v] = grayscales[v] + 1;
            }
        }
        this.lineWidth = mViewWidth * 1.0f / grayscales.length;
        this.mHistogramValues = grayscales;
        this.maxValue = getMax(grayscales);
        size = grayscales.length;
        for (int i = 0; i < size; i++) {
            float x = mViewWidth - (size - 1 - i) * lineWidth;

            Point point = new Point();
            point.setX(x);
            point.setY(mViewHeight - getHeightValue(grayscales[i]));

            this.points.add(point);
        }
        Point pointEnd = new Point();
        pointEnd.setX(mViewWidth);
        pointEnd.setY(mViewHeight);
        this.points.add(pointEnd);
        postInvalidate();
    }

    private float getHeightValue(int value) {
        return mViewHeight * 0.8f * value / maxValue;
    }

    private int getMax(int[] arr) {
        int max = 0;
        for (int i : arr) {
            if (i >= max)
                max = i;
        }
        return max;
    }

    private static class Point {
        float x;
        float y;

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }
    }
}
