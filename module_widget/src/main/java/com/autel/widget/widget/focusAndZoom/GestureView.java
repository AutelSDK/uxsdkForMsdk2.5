package com.autel.widget.widget.focusAndZoom;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.autel.common.model.lens.ITransLocation;
import com.autel.widget.R;

/**
 * @author dujiajie
 * @create 17.8.21
 **/
public class GestureView extends FrameLayout {

    public interface OpListener {
        /**
         * 进入/退出全屏
         *
         * @param full 是否是进入全屏
         */
        void fullScreenSwitch(boolean full);

        /**
         * 调整角度
         *
         * @param distance 正负值表示上下方身，距力表示力度
         */
        void changeGimbal(float distance);

        void setFocusPoint(float x, float y);

        void setLockFocus(boolean focused);

        void changeExposure(float value);

        void onScale(float scale);

        float currentEv();
    }

    private static final int FLAG_GIMBAL = 0x1;
    private static final int FLAG_FOCUS = 0x2;
    private static final int FLAG_SWITCH_SCREEN = 0x4;
    /**
     * 是否触发退出/进入全屏
     **/
    private static final int FLAG_DISTANCE = 0x100;
    /**
     * 聚焦组件是否锁定
     **/
    private static final int FLAG_LOCKED = 0x200;
    private static final int FLAG_FOCUS_FIRST = 0x400;

    private static final int FLAG_FUN_MASK = 0xF00;


    private int mFunFlag = FLAG_FOCUS | FLAG_GIMBAL | FLAG_SWITCH_SCREEN;
    private float mLastX, mLastY, mOriginY;
    //第二指
    private float mTouchX2, mTouchY2, mLastX2, mLastY2;

    private int mActivePointerId = -1;
    //是否是多指
    private boolean isMultPointer = false;

    private float mStepSize;

    private int mFixedFocusViewY, mGimbalViewY;
    private View mGimbalView;

    /**
     * 变焦，曝光度
     */
    private ChangeFocusView mFixedFocusView;
    private float mFocusOriginValue;

    private Drawable mGimbalDrawable;
    private Rect mFixedFocusBound = new Rect();
    private CheckForLongPress mCheckForLongPress;
    private CheckForViewDismiss mCheckForViewDismiss;

    private UpdateGimbalAngle mUpdateGimbalAngle;

    private OpListener mOpListener;
    private float mTouchSlop = 0f;
    private int mDistanceMinLength = 100;
    //需要限制的区域
    private RectF mValidateBounds = new RectF();
    private Paint mPaint = new Paint();
    private Path mPath = new Path();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private boolean isShowDashBox = false;
    private boolean isVerifyBounds = true;

    private float  mRegionLimitRatioX = 0.09277f;
    private float  mRegionLimitRatioY = 0.253906f;
    private float  mRegionLimitRatioW = 0.8134f;
    private float  mRegionLimitRatioH = 0.62453f;

    private boolean isSupportFocusLock = true;

    public GestureView(@NonNull Context context) {
        this(context, null, 0, 0);
    }

    public GestureView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0, 0);
    }

    public GestureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public GestureView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initViews(context);
    }

    public GestureView(@NonNull Context context, @Nullable ITransLocation transLocation) {
        super(context);
        initViews(context);
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void initViews(Context context) {
        mGimbalView = LayoutInflater.from(getContext()).inflate(R.layout.mission_item_drag_gimbal, this, false);
        mFixedFocusView = new ChangeFocusView(getContext());
        mFixedFocusView.setClickable(false);
        mFixedFocusView.setLongClickable(false);
        LayoutParams lp = new LayoutParams(context.getResources().getDimensionPixelSize(R.dimen.common_110dp), context.getResources().getDimensionPixelSize(R.dimen.common_110dp));
        lp.gravity = Gravity.START | Gravity.TOP;
        mFixedFocusView.setLayoutParams(lp);
        int parentSize = MeasureSpec.makeMeasureSpec(999, MeasureSpec.AT_MOST);
        measureChild(mGimbalView, parentSize, parentSize);
        measureChild(mFixedFocusView, parentSize, parentSize);

        Resources res = getResources();
        mGimbalDrawable = res.getDrawable(R.drawable.mission_ic_gimbal_tap, getContext().getTheme());
        mGimbalDrawable.setBounds(0, 0, mGimbalDrawable.getIntrinsicWidth(), mGimbalDrawable.getIntrinsicHeight());
        mStepSize = context.getResources().getDimensionPixelSize(R.dimen.common_20dp);
        mTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mDistanceMinLength = context.getResources().getDimensionPixelSize(R.dimen.common_72dp);
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(context.getResources().getDimensionPixelSize(R.dimen.common_1dp));
        mPaint.setColor(getResources().getColor(R.color.common_color_FEE15D));
        mPaint.setStyle(Paint.Style.STROKE);
        float dash = context.getResources().getDimensionPixelSize(R.dimen.common_12dp);
        DashPathEffect dashPathEffect = new DashPathEffect(new float[]{dash, dash}, 0);
        mPaint.setPathEffect(dashPathEffect);

    }

    /**
     * 是否校验手势操作
     */
    public void setVerifyBounds(boolean isVerifyBounds) {
        this.isVerifyBounds = isVerifyBounds;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (changed) {
            updateValidateBounds();
        }
    }

    public void updateValidateBounds() {
        int width = getWidth();
        int height = getHeight();
        float boundRegionLimitX = width * mRegionLimitRatioX;
        float boundRegionLimitH =height * mRegionLimitRatioY;

        mValidateBounds.set(boundRegionLimitX, boundRegionLimitH,  boundRegionLimitX + width * mRegionLimitRatioW, boundRegionLimitH + height * mRegionLimitRatioH);


        mPath.reset();
        mPath.addRect(mValidateBounds, Path.Direction.CW);
        setWillNotDraw(false);
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (mGimbalView.getParent() != null) {
            canvas.save();
            mGimbalDrawable.draw(canvas);
            canvas.restore();
        }
    }

    public void setOpListener(OpListener listener) {
        mOpListener = listener;
    }

    public void enableFixedFocus(boolean enable, boolean isSupportFocusLock) {
        if (enable) {
            mFunFlag |= FLAG_FOCUS;
        } else {
            mFunFlag &= ~FLAG_FOCUS;
        }
        this.isSupportFocusLock = isSupportFocusLock;
    }


    private void updateFixedViewDelay(int op) {
        if (mCheckForViewDismiss == null) {
            mCheckForViewDismiss = new CheckForViewDismiss(6000L);
        }
        mCheckForViewDismiss.reDelay(op);
    }

    public boolean checkAndUnLock() {
        if (mFixedFocusView.isLock() && mOpListener != null) {
            //取消 lock
            mOpListener.setLockFocus(false);
            mFixedFocusView.setLock(false);
            return true;
        }
        return false;
    }

    private void showFixedFocusView() {
        if ((mFunFlag & FLAG_FOCUS) == 0) {
            return;
        }
        if (mFixedFocusView.getParent() == null) {
            addView(mFixedFocusView);
        } else {
            checkAndUnLock();
        }
        mFixedFocusViewY = (int) mLastY;
        LayoutParams lp = (LayoutParams) mFixedFocusView.getLayoutParams();
        lp.leftMargin = (int) mLastX - mFixedFocusView.getMeasuredWidth() / 2;
        lp.topMargin = mFixedFocusViewY - mFixedFocusView.getMeasuredHeight() / 2;
//        float ev = mOpListener != null ? mOpListener.currentEv() : 0f;
        float ev = 0f; //测光时重置了ev
        mFixedFocusView.setLayoutParams(lp);
        mFixedFocusView.setValue(ev);
        mFixedFocusView.show();
        updateFixedViewDelay(1);
        if (mOpListener != null) {
            mOpListener.setFocusPoint(lp.leftMargin + mFixedFocusView.getMeasuredWidth() / 2, lp.topMargin + mFixedFocusView.getMeasuredHeight() / 2);
        }
    }

    public void removeFixedFocusView() {
        if (mFixedFocusView.getParent() == this) {
            removeView(mFixedFocusView);
        }
    }

    private void updateGimbalDrawable() {
        if (mGimbalView.getParent() == null) {
            return;
        }
        mFixedFocusBound.set(mGimbalDrawable.getBounds());
        mFixedFocusBound.offsetTo((int) mLastX - mFixedFocusBound.width() / 2, (int) mLastY - mFixedFocusBound.height() / 2);
        mGimbalDrawable.setBounds(mFixedFocusBound);
        invalidate();
    }

    public void switchLock() {
        if (mFixedFocusView.getParent() != null && (mFunFlag & FLAG_LOCKED) == 0) {
            boolean lock = !mFixedFocusView.isLock();
            if (mOpListener != null) {
                mOpListener.setLockFocus(lock);
            }
            mFixedFocusView.setLock(lock);

            updateFixedViewDelay(lock ? -1 : 1);
        }
    }

    public void setAELock(boolean aeLock, float locationX, float locationY) {
        if ((mFunFlag & FLAG_FOCUS) == 0) {
            return;
        }
        if (mFixedFocusView.getParent() == null) {
            addView(mFixedFocusView);
        }
        mFixedFocusViewY = (int) locationY;
        LayoutParams lp = (LayoutParams) mFixedFocusView.getLayoutParams();
        lp.leftMargin = (int) locationX - mFixedFocusView.getMeasuredWidth() / 2;
        lp.topMargin = mFixedFocusViewY - mFixedFocusView.getMeasuredHeight() / 2;
        mFixedFocusView.setLayoutParams(lp);
        mFixedFocusView.setValue(0);
        mFixedFocusView.showWithoutAnim();
        mFixedFocusView.setLock(aeLock);
    }

    private void onLongPress() {
        if (mFixedFocusView.getParent() == this && isSupportFocusLock) {
            mFixedFocusView.getHitRect(mFixedFocusBound);
            if ((mFunFlag & FLAG_LOCKED) == 0 && mFixedFocusBound.contains((int) mLastX, (int) mLastY)) {
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
                switchLock();
                mFunFlag |= FLAG_LOCKED;
            }
            return;
        }

        if ((mFunFlag & FLAG_GIMBAL) == 0) {
            return;
        }
        //TODO 禁用手势云台
//        if (mGimbalView.getParent() == null) {
//            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
//
//            LayoutParams lp = (LayoutParams) mGimbalView.getLayoutParams();
//            mGimbalViewY = (int) mLastY;
//            lp.topMargin = mGimbalViewY - mGimbalView.getMeasuredHeight() / 2;
//            lp.leftMargin = (int) mLastX - mGimbalView.getMeasuredWidth() / 2;
//            addView(mGimbalView);
//            updateGimbalDrawable();
//            removeFixedFocusView();
//        }
    }

    private void removeGimbal() {
        if (mGimbalView.getParent() == this) {
            removeView(mGimbalView);
        }
    }

    private boolean processMove(float x, float y) {
        if (mGimbalView.getParent() != null) {
            if (mGimbalViewY < y) {
                mGimbalView.setRotationX(180);
            } else mGimbalView.setRotationX(0);
            updateGimbalDrawable();
            if (mUpdateGimbalAngle == null) {
                mUpdateGimbalAngle = new UpdateGimbalAngle();
                post(mUpdateGimbalAngle);
            }
            return true;
        }
//        if (mFixedFocusView.getParent() != null) {
//            updateFixedViewDelay(1);
//
//            if ((mFunFlag & FLAG_FOCUS_FIRST) == 0) {
//                mFunFlag |= FLAG_FOCUS_FIRST;
//                mFocusOriginValue = mFixedFocusView.getValue();
//            }
//            float detal = (int) ((mOriginY - y) / mStepSize);
//            float value = Math.max(mFocusOriginValue + detal, mFixedFocusView.getMin());
//            value = Math.min(value, mFixedFocusView.getMax());
//            mFixedFocusView.setValue(value);
//            if (mOpListener != null) {
//                mOpListener.changeExposure(value);
//            }
////            Log.i("LLF", "detal:" + detal + ",value:" + value);
//            return true;
//        }
        return false;
    }

    private void processDistance(float detalY) {
//        Log.i("LLF", "processDistance " + detalY);
        if ((mFunFlag & (FLAG_DISTANCE)) == 0 && Math.abs(detalY) > mDistanceMinLength) {
            mFunFlag |= FLAG_DISTANCE;
            boolean isLandScape = getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
            if (mOpListener != null && (mFunFlag & FLAG_SWITCH_SCREEN) != 0 && isLandScape) {
                mOpListener.fullScreenSwitch(detalY > 0);
            }
        }
    }

    private void resetFlags() {
        mFunFlag &= ~FLAG_FUN_MASK;

        if (mUpdateGimbalAngle != null) {
            mUpdateGimbalAngle.stop();
            mUpdateGimbalAngle = null;
        }
//        mLastX = 0;
//        mLastY = 0;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
//        if (ev.getActionMasked() == MotionEvent.ACTION_DOWN) {
//            mLastEvent = ev;
//            mLastX = ev.getX();
//            mOriginY = mLastY = ev.getY();
//        }
        return mGimbalView.getParent() != null || super.onInterceptTouchEvent(ev);
    }

    private boolean validatePoint(float x, float y) {
        return mValidateBounds.isEmpty() || (x > mValidateBounds.left && x < mValidateBounds.right &&
                y > mValidateBounds.top && y < mValidateBounds.bottom);
    }

    private void singleTouchReset() {
        removeLongPressCheck();
        setPressed(false);
        removeGimbal();
        resetFlags();
    }

    private void computeScale(float x1, float y1, float x2, float y2) {
        if (mOpListener != null && (mFunFlag & FLAG_FOCUS) != 0) {
            float scale = (x2 * x2 + y2 * y2) / (x1 * x1 + y1 * y1);
//            Log.i("LLF", "computeScale x1:" + x1 + ",y1:" + y1 + ",x2:" + x2 + ",y2:" + y2 + ",rs:" + scale);
            mOpListener.onScale(scale);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isShowDashBox) {
            drawDashBox(canvas);
        }
    }

    public void hiddenAllView() {
        isShowDashBox = false;
        removeFixedFocusView();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        float x = event.getX();
        float y = event.getY();
        //过滤
        boolean rs = true;
        if (isVerifyBounds && !validatePoint(x, y)) {
            if (action == MotionEvent.ACTION_DOWN) {
                isShowDashBox = true;
                mHandler.removeCallbacksAndMessages(null);
                invalidate();
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        isShowDashBox = false;
                        invalidate();
                    }
                }, 3000);
                return false;
            }
            action = MotionEvent.ACTION_CANCEL;
            rs = false;
        }
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mLastX = event.getX(0);
                mOriginY = mLastY = event.getY(0);
                setPressed(true);
                isMultPointer = false;
                mActivePointerId = event.getPointerId(0);
                checkForLongPress();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                isMultPointer = true;
                singleTouchReset();
                mTouchX2 = event.getX(1);
                mTouchY2 = event.getY(1);
                mLastX2 = mTouchX2;
                mLastY2 = mTouchY2;
                break;
            case MotionEvent.ACTION_UP:
                if (!isMultPointer && isPressed()) {
                    showFixedFocusView();
                }
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                singleTouchReset();
                int pId = event.findPointerIndex(mActivePointerId);
                if (isMultPointer && pId >= 0) {
                    //多指
                    float x2 = event.getX(pId);
                    float y2 = event.getY(pId);
                    computeScale(mLastX - mTouchX2, mLastY - mTouchY2, x2 - mLastX2, y2 - mLastY2);
                }
                mActivePointerId = -1;
                break;
            case MotionEvent.ACTION_MOVE:
                int idx = event.findPointerIndex(mActivePointerId);
                if (!isMultPointer && idx >= 0) {
                    float x1 = event.getX(idx);
                    float y1 = event.getY(idx);
                    if (Math.abs(mLastX - x1) > mTouchSlop || Math.abs(mLastY - y1) > mTouchSlop) {
                        setPressed(false);
                        removeLongPressCheck();
                        if (processMove(x1, y1)) {
                            mLastX = x1;
                            mLastY = y1;
                        } else {
                            processDistance(mLastY - y1);
                        }
                    }
                } else if (idx < 0 && event.getPointerCount() > 1) {
                    //第二指
                    mLastX2 = event.getX(1);
                    mLastY2 = event.getY(1);
                }
                break;
        }
        return rs;
    }

    private void drawDashBox(Canvas canvas) {
        canvas.drawPath(mPath, mPaint);
    }

    private void checkForLongPress() {
        if (mCheckForLongPress == null) {
            mCheckForLongPress = new CheckForLongPress();
            mCheckForLongPress.rememberWindowAttachCount();
            postDelayed(mCheckForLongPress, ViewConfiguration.getLongPressTimeout());
        }
    }

    private void removeLongPressCheck() {
        if (mCheckForLongPress != null) {
            mCheckForLongPress.resetWindowAttachCount();
            removeCallbacks(mCheckForLongPress);
            mCheckForLongPress = null;
        }
    }

    private class CheckForViewDismiss implements Runnable {
        private long mDelayTime = 3000L;

        public CheckForViewDismiss(long delay) {
            this.mDelayTime = delay;
        }

        public void reDelay(int op) {
            removeCallbacks(this);
            if (op > 0) {
                postDelayed(this, mDelayTime);
            }
        }

        @Override
        public void run() {
            if (!mFixedFocusView.isLock()) {
                mCheckForViewDismiss = null;
                removeFixedFocusView();
            }
        }
    }

    private class CheckForLongPress implements Runnable {
        private int mWindowAttachCount = 0;

        public void resetWindowAttachCount() {
            mWindowAttachCount = 0;
        }

        public void rememberWindowAttachCount() {
            mWindowAttachCount = getWindowAttachCount();
        }

        @Override
        public void run() {
            if (mWindowAttachCount == getWindowAttachCount()) {
                mCheckForLongPress = null;
                setPressed(false);
                onLongPress();
            }
        }
    }

    private class UpdateGimbalAngle implements Runnable {
        private long mDelayTime = 150;
        private boolean mCancel = false;

        public void stop() {
            mCancel = true;
            removeCallbacks(this);
        }

        @Override
        public void run() {
            if (!mCancel && mOpListener != null) {
                float distance = mGimbalViewY - mLastY;
                if (Math.abs(distance) > mStepSize) {
                    float detal = distance / (getHeight() - mValidateBounds.bottom - mValidateBounds.top);
                    mOpListener.changeGimbal(detal);
                }
            }
            if (!mCancel) {
                postDelayed(this, mDelayTime);
            }
        }
    }
}
