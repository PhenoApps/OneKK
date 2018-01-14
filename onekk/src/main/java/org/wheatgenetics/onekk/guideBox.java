package org.wheatgenetics.onekk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.View;

/************************************************************************************
 * this class initiates the guide box with coins at the corners as marker references
 ************************************************************************************/

public class guideBox extends View {

    int COIN_SIZE;
    int COIN_BOX_SIZE;

    public guideBox(Context context, int coinSize) {
        // TODO Auto-generated constructor stub
        super(context);
        this.COIN_SIZE = 10-coinSize;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // TODO Auto-generated method stub
        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(3);
        paint.setColor(Color.GREEN);

        //center
        int x0 = canvas.getWidth() / 2;
        int y0 = canvas.getHeight() / 2;
        int dx = canvas.getHeight() / 3;
        int dy= canvas.getHeight() *10 / 20;

        //draw guide box
        canvas.drawRect(x0 - dx, y0 - dy, x0 + dx, y0 + dy, paint);
        try {
            COIN_BOX_SIZE = canvas.getWidth()/this.COIN_SIZE;
        }
        catch (Exception e){
            COIN_BOX_SIZE = canvas.getWidth()/6;
        }

        //draw guide boxes for coins
        canvas.drawRect(x0 - dx+COIN_BOX_SIZE, y0 - dy, x0+dx-COIN_BOX_SIZE,y0+dy, paint);
        canvas.drawRect(x0 - dx, y0 - dy+COIN_BOX_SIZE, x0 + dx, y0+dy-COIN_BOX_SIZE, paint);

        super.onDraw(canvas);
    }
}
