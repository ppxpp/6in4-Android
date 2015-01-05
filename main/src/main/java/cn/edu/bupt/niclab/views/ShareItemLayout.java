package cn.edu.bupt.niclab.views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

/**
 * Created by zhouhao on 2014/12/25.
 */
public class ShareItemLayout extends RelativeLayout {
    public ShareItemLayout(Context context) {
        super(context);
    }

    public ShareItemLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ShareItemLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //set height equals width
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
