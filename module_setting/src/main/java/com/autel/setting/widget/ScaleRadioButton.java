package com.autel.setting.widget;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.autel.setting.R;

/**
 * @date 2022/11/17.
 * @author gaojie
 * @description 可改变Drawable大小的RadioButton
 */

public class ScaleRadioButton extends androidx.appcompat.widget.AppCompatRadioButton {
    private int mDrawableSize;//drawable大小

    public ScaleRadioButton(Context context) {
        this(context, null);
    }

    public ScaleRadioButton(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ScaleRadioButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Drawable drawableLeft = null;
        Drawable drawableRight = null;
        Drawable drawableBottom = null;
        Drawable drawableTop = null;

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ScaleRadioButton);//获取自定义属性
        int n = a.getIndexCount();
        for (int i = 0; i < n; i++) {
            int attr = a.getIndex(i);
            if (attr == R.styleable.ScaleRadioButton_drawableSizes) {
                mDrawableSize = a.getDimensionPixelSize(R.styleable.ScaleRadioButton_drawableSizes, 25);//获取drawable大小，没设置时默认为50dp
            } else if (attr == R.styleable.ScaleRadioButton_drawableTop) {
                drawableTop = a.getDrawable(attr);//获取显示在上方的drawable
            } else if (attr == R.styleable.ScaleRadioButton_drawableBottom) {
                drawableBottom = a.getDrawable(attr);//获取显示在下方的drawable
            } else if (attr == R.styleable.ScaleRadioButton_drawableRight) {
                drawableRight = a.getDrawable(attr);//获取显示在右方的drawable
            } else if (attr == R.styleable.ScaleRadioButton_drawableLeft) {
                drawableLeft = a.getDrawable(attr);//获取显示在左方的drawable
            }
        }
        a.recycle();
        setCompoundDrawablesWithIntrinsicBounds(drawableTop, drawableBottom, drawableLeft, drawableRight);//调用重写的方法进行设置drawable的大小.
    }


    //重写TextView中的setCompoundDrawablesWithIntrinsicBounds的方法
    @Override
    public void setCompoundDrawablesWithIntrinsicBounds(Drawable top, Drawable bottom, Drawable left, Drawable right) {
        if (left != null) {
            left.setBounds(0, 0, mDrawableSize, mDrawableSize);//drawable的bounds设置为了我们自定义大小了，第三个参数是设置宽度，第四个参数是设置高度，这里我就只设置了同一个也就是正方形了，如果想设置矩形的可以声明2个不同的自定义属性来设置。
        }
        if (right != null) {
            right.setBounds(0, 0, mDrawableSize, mDrawableSize);
        }
        if (top != null) {
            top.setBounds(0, 0, mDrawableSize, mDrawableSize);
        }
        if (bottom != null) {
            bottom.setBounds(0, 0, mDrawableSize, mDrawableSize);
        }
        setCompoundDrawables(left, top, right, bottom);//设置完了bounds就可以调用这个方法进行设置了 不用在代码中进行动态控制了。
    }
}
