package org.wheatgenetics.onekk;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.TextureView;

/**
 * A {@link TextureView} that can be adjusted to a specified aspect ratio.
 */
public class AutoFitTextureView extends TextureView {

    private int mRatioWidth = 0;
    private int mRatioHeight = 0;

    public AutoFitTextureView(Context context) {
        this(context, null);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AutoFitTextureView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    public void setAspectRatio(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Size cannot be negative.");
        }
        mRatioWidth = width;
        mRatioHeight = height;

        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int screenWidth = widthMeasureSpec;
        int screenHeight = heightMeasureSpec;
        int finalWidth = screenWidth;
        int finalHeight = screenHeight;
        int widthDifference = 0;
        int heightDifference = 0;
        float screenAspectRatio = (float) screenWidth / screenHeight;
        float cameraAspectRatio = (float) mRatioWidth/ mRatioHeight;

        if (screenAspectRatio > cameraAspectRatio) { //Keep width crop height
            finalHeight = (int) (screenWidth / cameraAspectRatio);
            heightDifference = finalHeight - screenHeight;
        } else { //Keep height crop width
            finalWidth = (int) (screenHeight * cameraAspectRatio);
            widthDifference = finalWidth - screenWidth;
        }


        setMeasuredDimension((finalWidth - widthDifference)/ 2, (finalHeight - heightDifference)/ 2);

//        int width = MeasureSpec.getSize(widthMeasureSpec);
//        int height = MeasureSpec.getSize(heightMeasureSpec);
//        if (0 == mRatioWidth || 0 == mRatioHeight) {
//            setMeasuredDimension(width, height);
//        } else {
//            if (width < height * mRatioWidth / mRatioHeight) {
//                setMeasuredDimension(width, width * mRatioHeight / mRatioWidth);
//            } else {
//                setMeasuredDimension(height * mRatioWidth / mRatioHeight, height);
//            }
//        }
    }

}
