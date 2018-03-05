package org.wheatgenetics.onekk;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;
import android.view.View;

/************************************************************************************
 * this class initiates the guide box with coins at the corners as marker references
 ************************************************************************************/

public class guideBox extends View {

    int screenWidth, screenHeight;
    int COIN_SIZE;
    int COIN_BOX_SIZE;
    Paint paint;
    DisplayMetrics displayMetrics;
    Rect gridRect;

    public guideBox(Context context, int coinSize) {
        // TODO Auto-generated constructor stub
        super(context);
        gridRect = new Rect();

        this.COIN_SIZE = 10-coinSize;
        displayMetrics = Resources.getSystem().getDisplayMetrics();

        screenWidth = displayMetrics.widthPixels;
        screenHeight = (int) (displayMetrics.heightPixels*0.9);
        paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.GREEN);
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

        /*try {
            COIN_BOX_SIZE = canvas.getWidth()/this.COIN_SIZE;
        }
        catch (Exception e){
            COIN_BOX_SIZE = canvas.getWidth()/6;
        }

        //draw guide boxes for coins
        canvas.drawRect(x0 - dx+COIN_BOX_SIZE, y0 - dy, x0+dx-COIN_BOX_SIZE,y0+dy, paint);
        canvas.drawRect(x0 - dx, y0 - dy+COIN_BOX_SIZE, x0 + dx, y0+dy-COIN_BOX_SIZE, paint);
        */

        super.onDraw(canvas);
    }
}
