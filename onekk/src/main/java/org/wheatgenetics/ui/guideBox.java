package org.wheatgenetics.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;

/************************************************************************************
 * this class initiates the guide box with coins at the corners as marker references
 ************************************************************************************/

public class guideBox extends View {

    int screenWidth, screenHeight;
    int imageWidth, imageHeight;
    double coinSize, coinSizeInPx;
    Paint paint;
    Paint coinPaint;
    DisplayMetrics displayMetrics;
    Rect gridRect;
    ArrayList<Point> coinCoordsList;
    Matrix rotationMatrix = new Matrix();

    public guideBox(Context context){super(context);}

    public guideBox(Context context, int coinSize) {
        // TODO Auto-generated constructor stub
        super(context);
        gridRect = new Rect();
        this.coinSize = coinSize;

        displayMetrics = Resources.getSystem().getDisplayMetrics();
        screenWidth = displayMetrics.widthPixels;
        screenHeight = (int) (displayMetrics.heightPixels*0.9);

        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.GREEN);

        coinPaint = new Paint();
        coinPaint.setStyle(Paint.Style.FILL);
        coinPaint.setStrokeWidth(3);
        coinPaint.setColor(Color.RED);
        coinPaint.setTextSize(30);
    }

    @Override
    protected void onDraw(Canvas canvas) {

        // TODO Auto-generated method stub
        //center
        int x0 = canvas.getWidth() / 2;
        int y0 = canvas.getHeight() / 2;
        int dx = (9 * canvas.getWidth()) / 20;
        int dy = (9 * canvas.getHeight()) / 20;

        gridRect.set(x0 - dx, y0 - dy, x0 + dx, y0 + dy);

        //draw guide box
        canvas.drawRect(gridRect, paint);
        drawCoin(canvas);
        super.onDraw(canvas);
    }

    private void drawCoin(Canvas canvas){
        if(coinCoordsList != null){
            if(coinCoordsList.size() > 0) {

                float scaleFactorX = ((float)screenWidth)/imageHeight;
                float scaleFactorY = ((float)screenHeight)/imageWidth;

                Log.d("Screen dimensions",String.format("%d, %d",screenWidth, screenHeight));
                Log.d("Image dimensions",String.format("%d, %d",imageWidth, imageHeight));
                Log.d("Scale factors",String.format("%f, %f",scaleFactorX, scaleFactorY));

                rotationMatrix.reset();

                /* rotating the centroid coordinated 90 degrees clockwise */
                //rotationMatrix.postRotate(90);

                for (Point point : coinCoordsList) {

                    float tempPts[] = {point.x, point.y};
                    rotationMatrix.mapPoints(tempPts);

                    float ptX = Math.abs(tempPts[0]);
                    float ptY = Math.abs(tempPts[1]);

                    Log.d("Rotated Coordinates",String.format("%f, %f",ptX,ptY));

                    coinPaint.setColor(Color.RED);
                    canvas.drawCircle(ptX, ptY, 20, this.coinPaint);
                    canvas.drawText((int) ptX + " " + (int) ptY, ptX + 2, ptY + 2, this.coinPaint);
                }

                /* scaling the centroid coordinates by scaleFactorX and scaleFactorY */
                rotationMatrix.postScale(scaleFactorY,scaleFactorX);

                for (Point point : coinCoordsList) {

                    float tempPts[] = {point.x,point.y};
                    rotationMatrix.mapPoints(tempPts);

                    float ptX = Math.abs(tempPts[0]);
                    float ptY = Math.abs(tempPts[1]);

                    Log.d("Scaled Coordinates",String.format("%f, %f",ptX,ptY));

                    coinPaint.setColor(Color.CYAN);
                    canvas.drawCircle(ptX, ptY, 20, this.coinPaint);
                    canvas.drawText((int)ptX + " " + (int)ptY,ptX+2,ptY+2 ,this.coinPaint);
                }
            }
        }
    }

    public void setCoinCoordsList(ArrayList<Point> coinCoordsList) {
        this.coinCoordsList = coinCoordsList;
    }

    public int getImageWidth() {
        return imageWidth;
    }

    public void setImageWidth(int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return imageHeight;
    }

    public void setImageHeight(int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public Rect getGridRect() {
        return gridRect;
    }

    public double getCoinSize() {
        return coinSize;
    }

    public double getCoinSizeInPx() {
        coinSizeInPx = (coinSize * displayMetrics.densityDpi)/ 25.4;
        return coinSizeInPx;
    }

    public int getScreenWidth() {
        return screenWidth;
    }

    public int getScreenHeight() {
        return screenHeight;
    }
}
