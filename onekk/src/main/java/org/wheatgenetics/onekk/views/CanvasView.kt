package org.wheatgenetics.onekk.views

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceView
import android.view.View
import androidx.camera.view.PreviewView

class CanvasView(context: Context?) : PreviewView(context!!) {

    constructor(context: Context?, attrs: AttributeSet?) : this(context) {

    }

    constructor(context: Context?, attrs: AttributeSet?, defStyle: Int) : this(context, attrs) {

    }

    var rect: RectF = RectF(20f, 20f, 100f, 100f)
    var mTextPaint: Paint = Paint()
//    var mCanvas: Canvas? = null
    //var bitmap: Bitmap? = null
//            Bitmap.createBitmap(1080, 1920, Bitmap.Config.ALPHA_8)
//        get() {
//            return field
//        }
//        set(value) {
//            field = Bitmap.createScaledBitmap(value, 1080, 1920, false)
//        }

//    var mPoints: ArrayList<PointF>

//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        Log.d("Canvas", "Draw")
//        //mPaint.isFilterBitmap = truef
////        bitmap?.let { bmp ->
////            canvas.drawBitmap(bmp, 1080f, 1920f, null)
////        }
//
//        //canvas.drawColor(0x00AAAAAA)
//        //canvas.drawRect(0f, 0f, 1080f, 1920f, mPaint)
//        canvas.drawText("Coin Recognition.", 100f, 200f, mTextPaint)
////        if (mPoints.size() > 0) {
////            for (p in mPoints) {
////                canvas.drawCircle(p.x, p.y, 5, mPaint)
////            }
////        }
////        if (mPoints.size() === 3) {
////            val first: PointF = mPoints[0]
////            val second: PointF = mPoints[1]
////            val third: PointF = mPoints[2]
////            canvas.drawLine(first.x, first.y, second.x, second.y, mPaint)
////            canvas.drawLine(first.x, first.y, third.x, third.y, mPaint)
////            val angle = Math.toDegrees(Math.atan2(third.x - first.x, third.y - first.y) -
////                    Math.atan2(second.x - first.x, second.y - first.y))
////            canvas.drawText(angle.toString(), 100, 400, mPaint)
////        }
//        //canvas.drawOval(rect, mPaint);
//    }
//
//    override fun onTouchEvent(@NonNull event: MotionEvent): Boolean {
//        val x: Float = event.getX()
//        val y: Float = event.getY()
//        when (event.getAction()) {
//            MotionEvent.ACTION_DOWN -> {
////                if (mPoints.size() < 3) mPoints.add(PointF(x, y)) else {
////                    mPoints.clear()
////                    mPoints.add(PointF(x, y))
////                }
//                invalidate()
//            }
//            MotionEvent.ACTION_MOVE -> {
//            }
//            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
//            }
//        }
//        return true
//    }

    init {
//        mPoints = ArrayList()
        mTextPaint.textSize = 64f
        //mCanvas = new Canvas(mutable);
    }
}