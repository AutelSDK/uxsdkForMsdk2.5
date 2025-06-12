package com.autel.widget.widget.histogram;

import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import androidx.annotation.Size;

import com.autel.log.AutelLog;
import com.autel.widget.R;

public class HistogramWindowManager {

    private final Context context;

    private WindowManager mWindowManager;

    private HistogramView histogramView;

    private boolean isWindowCreated;

    private onHistogramWindowManagerListener onHistogramWindowManagerListener;

    private String lensName = "";

    public interface onHistogramWindowManagerListener {
        void onHistogramViewCancel();
    }

    public void setOnHistogramWindowManagerListener(HistogramWindowManager.onHistogramWindowManagerListener onHistogramWindowManagerListener) {
        this.onHistogramWindowManagerListener = onHistogramWindowManagerListener;
    }

    public HistogramWindowManager(Context context) {
        this.context = context;
    }

    private void createHistogramView(@Size(2) int[] location) {
        if (isWindowCreated) {
            return;
        }

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();

        lp.format = PixelFormat.TRANSLUCENT;
        lp.flags = WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM;
        lp.width = (int) (context.getResources().getDimension(R.dimen.common_160dp));
        lp.height = (int) (context.getResources().getDimension(R.dimen.common_110dp));

        lp.x = location[0];
        lp.y = location[1] - lp.height;
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.alpha = 0.8f;

        histogramView = new HistogramView(context);
        histogramView.setViewParams(lp);
        histogramView.updateHistogramName(lensName);
        histogramView.setOnHistogramViewListener(new HistogramView.OnHistogramViewListener() {
            @Override
            public void onHistogramViewCancel() {
                if (null != onHistogramWindowManagerListener) {
                    onHistogramWindowManagerListener.onHistogramViewCancel();
                }

                hiddenHistogramWindow();
            }
        });

        getWindowManager(context).addView(histogramView, lp);

        isWindowCreated = true;
    }

    public void setOnViewLocationChangeListener(HistogramView.OnViewLocationChangeListener mOnViewLocationChangeListener) {
        if (histogramView != null) {
            histogramView.setOnViewLocationChangeListener(mOnViewLocationChangeListener);
        }
    }

    public void destroy() {
        this.onHistogramWindowManagerListener = null;
        if (histogramView != null) {
            try {
                getWindowManager(context).removeView(histogramView);
            } catch (Exception e) {
                AutelLog.e("tag_Exception", e.toString());
            }
        }
        mWindowManager = null;
        isWindowCreated = false;
    }

    public void showHistogramWindow(@Size(2) int[] location) {
        createHistogramView(location);
        histogramView.setVisibility(View.VISIBLE);
    }

    public void hiddenHistogramWindow() {
        if (null != histogramView) {
            histogramView.setVisibility(View.GONE);
        }
        destroy();
    }

    public static void clearHistogramWindowPosition() {
    }

    public void saveHistogramWindowPosition() {
        try {
            if (histogramView != null) {
                int[] positions = histogramView.getPosition();

                if (positions != null && positions.length > 1) {

                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void updateNewHistogramValues(int[] values) {
        if (null != histogramView && histogramView.isShown()) {
            histogramView.updateHistogramValues(values);
        }
    }

    public void updateHistogramName(String lenName) {
        if (null != histogramView && histogramView.isShown()) {
            histogramView.updateHistogramName(lenName);
        }
        this.lensName = lenName;
    }

    /**
     * 如果WindowManager还未创建，则创建一个新的WindowManager返回。否则返回当前已创建的WindowManager。
     *
     * @param context 必须为应用程序的Context.
     * @return WindowManager的实例，用于控制在屏幕上添加或移除悬浮窗。
     */
    private WindowManager getWindowManager(Context context) {
        if (mWindowManager == null) {
            mWindowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }
        return mWindowManager;
    }

}
