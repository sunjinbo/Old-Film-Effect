package com.oldfilm.render;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.SurfaceView;

import com.oldfilm.R;

public class VideoView extends SurfaceView {
    private int aspect_ratio_width;
    private int aspect_ratio_height;
    private float ratio = 1;

    public VideoView(Context context) {
        super(context);
    }

    public VideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        showAttrs(context, attrs);
    }

    private void showAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.MyTextViewAttr);
        aspect_ratio_width = typedArray.getInt(R.styleable.MyTextViewAttr_aspect_ratio_width, 0);
        aspect_ratio_height = typedArray.getInt(R.styleable.MyTextViewAttr_aspect_ratio_height, 0);
        typedArray.recycle();
        if (aspect_ratio_width == 0 || aspect_ratio_height == 0) {
            ratio = 1;
        }else {
            ratio = (float) aspect_ratio_width / (float) aspect_ratio_height;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (ratio==1) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int originalWidth = MeasureSpec.getSize(widthMeasureSpec);
        int originalHeight = MeasureSpec.getSize(heightMeasureSpec);

        int calculatedHeight = (int) (originalWidth / ratio);
        int finalWidth, finalHeight;

        if (calculatedHeight > originalHeight) {
            finalWidth = (int) (originalHeight * ratio);
            finalHeight = originalHeight;
        } else {
            finalWidth = originalWidth;
            finalHeight = calculatedHeight;
        }
        super.onMeasure(
                MeasureSpec.makeMeasureSpec(finalWidth, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(finalHeight, MeasureSpec.EXACTLY));

    }

    public void setAspectWithWidthHeight(float ratio) {
        this.ratio = ratio;
    }
}