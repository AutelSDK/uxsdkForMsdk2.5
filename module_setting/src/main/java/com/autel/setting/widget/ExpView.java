package com.autel.setting.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathEffect;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.autel.setting.R;

/**
 * exp调节View
 * 绘制流程如下：
 * 1.绘制虚线边框、绘制虚线对角线、绘制X与Y轴线
 * 2.通过delta值，来确定二阶贝塞尔的三个点坐标，如果delta = 0.5则为一条直线，左下与右上为两段二阶贝塞尔线
 * 3.通过upDownValue(蓝色点)、leftRightValue(橙色点)值来确定点在贝塞尔线上的位置，并绘制沿伸到X,Y轴上的两条虚线
 * 4.可通过触摸移动调节delta值(左下、右下delta值增大，左上、右上delta值减小)
 */
public class ExpView extends View {

    public static final float MIN = 0.2f;
    public static final float MAX = 0.7f;
    private Paint mPaint;
    private final PathEffect effect = new DashPathEffect(new float[]{6, 6}, 0);
    private Path mPath;
    private int borderColor, dividerColor, lineColor, orange;
    private float px1, px6, px10;
    private RectF rectF;
    private Rect rect;
    private float delta = 0.5f;
    private float startX, startY;
    private static final int MIDDLE_VALUE = 0; //摇杆中值
    private static final float DELTA = 100f;
    private int upDownValue = -999;
    private int leftRightValue = -999;
    private Point point0, point1, point2;
    private OnExpChangeListener listener;

    private enum TouchLocation {
        LEFT, RIGHT
    }

    private TouchLocation touchLocation;

    public ExpView(Context context) {
        this(context, null);
    }

    public ExpView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ExpView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setOnExpChangeListener(OnExpChangeListener listener) {
        this.listener = listener;
    }

    private void init() {
        setFocusable(true);
        setFocusableInTouchMode(true);
        setClickable(true);

        borderColor = getContext().getResources().getColor(R.color.common_color_33d7d7d9);
        dividerColor = getContext().getResources().getColor(R.color.common_color_7f7f7f);
        lineColor = getContext().getResources().getColor(R.color.common_color_FEE15D);
        orange = getContext().getResources().getColor(R.color.common_color_white);
        px1 = getContext().getResources().getDimension(R.dimen.common_1dp);
        px6 = getContext().getResources().getDimension(R.dimen.common_6dp);
        px10 = getContext().getResources().getDimension(R.dimen.common_10dp);

        mPaint = new Paint();
        mPath = new Path();
        rectF = new RectF();
        rect = new Rect();
        point0 = new Point();
        point1 = new Point();
        point2 = new Point();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        //关闭硬件加速，避免部分设备绘制虚线无效
        setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        float beginX = px6 / 2f;
        float beginY = px6 / 2f;
        float endX = getWidth() - px6 / 2f;
        float endY = getHeight() - px6 / 2f;

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setColor(borderColor);
        mPaint.setStrokeWidth(px1);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setPathEffect(effect);
        int width = getWidth();
        int height = getHeight();
        rectF.left = beginX;
        rectF.top = beginY;
        rectF.right = endX;
        rectF.bottom = endY;
        canvas.drawRect(rectF, mPaint);

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setColor(dividerColor);
        mPaint.setStrokeWidth(px1);
        canvas.drawLine(beginX, height / 2f, endX, height / 2f, mPaint);
        canvas.drawLine(width / 2f, beginY, width / 2f, endY, mPaint);

        mPaint.setColor(dividerColor);
        mPaint.setPathEffect(effect);
        canvas.drawLine(beginX, endY, endX, beginY, mPaint);

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setColor(dividerColor);
        mPaint.setStrokeWidth(px1);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setTextSize(px10);
        String textX = "X";
        mPaint.getTextBounds(textX, 0, textX.length(), rect);
        canvas.drawText(textX, endX - rect.width() - 2 * px1, height / 2f + rect.height() + 2 * px1, mPaint);

        String textY = "Y";
        mPaint.getTextBounds(textY, 0, textY.length(), rect);
        canvas.drawText(textY, width / 2f - rect.width() - 2 * px1, beginY + rect.height() + 2 * px1, mPaint);

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setColor(lineColor);
        mPaint.setStrokeWidth(px1);
        mPaint.setStyle(Paint.Style.STROKE);
        mPath.reset();
        mPath.moveTo(beginX, endY);
        mPath.quadTo(delta * width / 2f, height / 2f + delta * height / 2, width / 2f, height / 2f);
        canvas.drawPath(mPath, mPaint);
        mPath.reset();
        mPath.moveTo(width / 2f, height / 2f);
        mPath.quadTo(width / 2f + (1 - delta) * width / 2f, (1 - delta) * height / 2f, endX, beginY);
        canvas.drawPath(mPath, mPaint);

        if (leftRightValue > -999) {
            drawGas(leftRightValue, orange, canvas, width, height, beginX, beginY, endX, endY);
        }

        if (upDownValue > -999) {
            drawGas(upDownValue, lineColor, canvas, width, height, beginX, beginY, endX, endY);
        }
    }

    private void drawGas(int gas, int color, Canvas canvas, int width, int height, float beginX,
                         float beginY, float endX, float endY) {
        if (gas <= MIDDLE_VALUE) {
            point0.x = beginX;
            point0.y = endY;
            point1.x = delta * width / 2f;
            point1.y = height / 2f + delta * height / 2f;
            point2.x = width / 2f;
            point2.y = height / 2f;
        } else {
            point0.x = endX;
            point0.y = beginY;
            point1.x = width / 2f + (1 - delta) * width / 2f;
            point1.y = (1 - delta) * height / 2f;
            point2.x = width / 2f;
            point2.y = height / 2f;
        }
        Point point = getBezierDot(1f - Math.abs(gas - MIDDLE_VALUE) / DELTA, point0, point1, point2);
        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(color);
        canvas.drawCircle(point.x, point.y, px6 / 2f, mPaint);

        mPaint.reset();
        mPaint.setAntiAlias(true);
        mPaint.setColor(color);
        mPaint.setStrokeWidth(px1);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setPathEffect(effect);
        canvas.drawLine(point.x, height / 2f, point.x, point.y, mPaint);
        canvas.drawLine(width / 2f, point.y, point.x, point.y, mPaint);
    }

    public void setExp(float delta) {
        this.delta = delta;
        invalidate();
    }

    public void updateUpDown(int upDownValue) {
        this.upDownValue = upDownValue;
        invalidate();
    }

    public float getExp() {
        return delta;
    }

    public void updateLeftRight(int leftRightValue) {
        this.leftRightValue = leftRightValue;
        invalidate();
    }

    public void updateAround(int upDownValue, int leftRightValue) {
        this.upDownValue = upDownValue;
        this.leftRightValue = leftRightValue;
        invalidate();
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {

        final int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                startX = event.getX();
                startY = event.getY();
                checkTouchLocation();
            }
            break;

            case MotionEvent.ACTION_MOVE: {
                float curX = event.getX();
                float curY = event.getY();
                //double moveDistance = Math.sqrt(Math.pow(curX - startX, 2) + Math.pow(curY - startY, 2));
                if (touchLocation == TouchLocation.RIGHT) {
                    if (curY >= startY && delta > MIN) {
                        delta += -0.01;
                        startX = curX;
                        startY = curY;
                    } else if (curY < startY && delta < MAX) {
                        delta += 0.01;
                        startX = curX;
                        startY = curY;
                    }

                } else {
                    if (curY >= startY && delta < MAX) {
                        delta += 0.01;
                        startX = curX;
                        startY = curY;
                    } else if (curY < startY && delta > MIN) {
                        delta += -0.01;
                        startX = curX;
                        startY = curY;
                    }
                }
                delta = Math.max(delta, MIN);
                delta = Math.min(delta, MAX);
                invalidate();
                if (null != listener) {
                    listener.expChange(delta, false);
                }
            }
            break;

            case MotionEvent.ACTION_UP: {
                if (null != listener) {
                    listener.expChange(delta, true);
                }
            }
            break;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }

        return super.dispatchTouchEvent(event);
    }

    private void checkTouchLocation() {
        if (startX > 0 && startX < getWidth() / 2f) {
            touchLocation = TouchLocation.LEFT;
        } else {
            touchLocation = TouchLocation.RIGHT;
        }
    }

    public interface OnExpChangeListener {
        void expChange(float exp, boolean isSend);
    }

    private Point getBezierDot(float t, Point point0, Point point1, Point point2) {
        float x = (1 - t) * (1 - t) * point0.x + 2 * t * (1 - t) * point1.x + t * t * point2.y;
        float y = (1 - t) * (1 - t) * point0.y + 2 * t * (1 - t) * point1.y + t * t * point2.y;
        return new Point(x, y);
    }

    private static class Point {
        float x;
        float y;

        Point() {
        }

        Point(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }
}
