package com.autel.widget.widget.histogram;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.autel.widget.R;


public class HistogramView extends RelativeLayout {

    private WindowManager windowManager;

    private RelativeLayout layout_histogram_cancel;

    private float xViewOnDown = 0.0f;
    private float yViewOnDown = 0.0f;
    private float xOnDown = 0.0f;
    private float yOnDown = 0.0f;
    private float xMove = 0.0f;
    private float yMove = 0.0f;

    private HistogramChartView chartView;
    private TextView tvLensName;

    /**
     * 小悬浮窗的参数
     */
    private WindowManager.LayoutParams mParams;

    private OnHistogramViewListener onHistogramViewListener;

    private OnViewLocationChangeListener mOnViewLocationChangeListener;

    public interface OnHistogramViewListener {
        void onHistogramViewCancel();
    }

    public void setOnHistogramViewListener(OnHistogramViewListener onHistogramViewListener) {
        this.onHistogramViewListener = onHistogramViewListener;
    }

    public void setViewParams(WindowManager.LayoutParams mParams) {
        this.mParams = mParams;
    }

    public int[] getPosition() {
        return this.mParams != null ? new int[]{this.mParams.x, this.mParams.y} : null;
    }

    public HistogramView(Context context) {
        super(context);
        init(context);
    }

    public HistogramView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HistogramView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        this.setBackgroundResource(R.drawable.mission_shape_roundrect_black_alpha55);
        initWindowManager(context);

        LayoutInflater.from(context).inflate(R.layout.widget_histogram_view, this, true);
        layout_histogram_cancel = (RelativeLayout) findViewById(R.id.layout_histogram_cancel);
        chartView = (HistogramChartView) findViewById(R.id.view_chart);
        tvLensName = (TextView) findViewById(R.id.tv_lens_name);

        layout_histogram_cancel.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (onHistogramViewListener != null) {
                    onHistogramViewListener.onHistogramViewCancel();
                }
            }
        });
    }

    public void setOnViewLocationChangeListener(OnViewLocationChangeListener mOnViewLocationChangeListener) {
        this.mOnViewLocationChangeListener = mOnViewLocationChangeListener;
    }

    private void initWindowManager(Context context) {
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
    }

    public void updateHistogramValues(int[] values) {
        chartView.updateHistogramValues(values);
    }

    public void updateHistogramName(String lenName) {
        tvLensName.setText(lenName);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                xViewOnDown = event.getRawX() - event.getX();//窗口左上角在屏幕的位置
                yViewOnDown = event.getRawY() - event.getY();
                xOnDown = event.getRawX();
                yOnDown = event.getRawY();
                break;

            case MotionEvent.ACTION_MOVE:
                xMove = event.getRawX() - xOnDown;//移动的绝对距离
                yMove = event.getRawY() - yOnDown;
                mParams.x = (int) (xViewOnDown + xMove);
                mParams.y = (int) (yViewOnDown + yMove);
                if (mOnViewLocationChangeListener != null) {
                    mOnViewLocationChangeListener.onChange(mParams.x, mParams.y + getHeight());
                }
                try {
                    windowManager.updateViewLayout(this, mParams);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
                break;

            case MotionEvent.ACTION_UP:
                // 如果手指离开屏幕时，xDownInScreen和xInScreen相等，且yDownInScreen和yInScreen相等，则视为触发了单击事件。
                break;
            default:
                break;
        }
        return true;
    }

    public interface OnViewLocationChangeListener {
        void onChange(int x, int y);
    }

}
