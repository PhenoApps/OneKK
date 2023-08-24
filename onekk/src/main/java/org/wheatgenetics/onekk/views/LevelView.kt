package org.wheatgenetics.onekk.views

import android.content.Context
import android.content.Context.SENSOR_SERVICE
import android.content.Context.VIBRATOR_SERVICE
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.hardware.*
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import org.wheatgenetics.onekk.R
import kotlin.math.abs

/**
 * @author Chaney 11/17/2021
 * A custom frame layout that visualizes the device's orientation.
 *
 * The inflated layout includes a 'reticle' which is a static circle and cross that
 * designates the flat surface.
 *
 * A dynamic calibration cross is drawn using the device accelerometer and magnetometer.
 * When the calibration cross is within #mCalibrationThreshold degrees of the reticle, calibration is
 * not automatically accepted, instead a counter is used to check that the threshold is kept within
 * mSuccessThreshold canvas draws. When the threshold is met, a small vibration happens and the
 * visibility changes to invisible. The view will become visible if the #mCalibrationThreshold
 * is not met.
 *
 */
class LevelView(ctx: Context, attributeSet: AttributeSet?): FrameLayout(ctx, attributeSet),
    SensorEventListener {

    //here is where the layout is inflated
    private val mRoot: View = inflate(context, R.layout.view_level, this)

    //this is the image view that has the drawable R.drawable.gyroscope_center_reticle
    private val mReticle: ImageView = mRoot.findViewById(R.id.gyroscope_center_iv)

    //sensors and vibrator are initialized in init
    private val mSensorManager: SensorManager = context.getSystemService(SENSOR_SERVICE) as SensorManager
    private val mVibrator: Vibrator

    //yellow dot in center of calibration cross
    private val mCalibrationCenterPoint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        isAntiAlias = true
        strokeWidth = resources.getDimension(R.dimen.calibration_point_size_success)
    }

    //white dot in center of the reticle
    private val mReticleCenterPoint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.WHITE
        isAntiAlias = true
        strokeWidth = resources.getDimension(R.dimen.calibration_point_size_success)
    }

    //the calibration cross color when un calibrated
    private val mCalibrationPointPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        isAntiAlias = true
        strokeWidth = resources.getDimension(R.dimen.calibration_point_size)
    }

    //the calibration cross color when calibrated
    private val mCalibrationSuccessPointPaint: Paint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.GREEN
        isAntiAlias = true
        strokeWidth = resources.getDimension(R.dimen.calibration_point_size_success)
    }

    //track number of draws calibrations is successful and make reticle invisible
    private var mSuccessCounter = 0
    private val mSuccessThreshold = 16
    private var mCalibrated = false
    private val mCalibrationThreshold = 5

    private val mCalibrationLineLength = resources.getDimension(R.dimen.calibration_line_length)
    private var mGravity: FloatArray? = null
    private var mGeomagneticField: FloatArray? = null
    private var mOrientation: FloatArray? = null

    init {

        //clear this flag to allow onDraw to be called
        setWillNotDraw(false)

        //initialize the vibrator and register listeners for the sensors
        mVibrator = context.getSystemService(VIBRATOR_SERVICE) as Vibrator

        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
            SensorManager.SENSOR_DELAY_UI)

        mSensorManager.registerListener(this,
            mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
            SensorManager.SENSOR_DELAY_UI)

    }

    /**
     * Unregisters the sensor manager when this view is detached.
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        mSensorManager.unregisterListener(this)

    }

    /**
     * Given a view it uses the width, height and location to determine the pixel center.
     */
    private fun View.center(): Point = Point().apply {

        //get top left x, y coordinates
        val left = this@center.x
        val top = this@center.y

        //get width and height of view
        val width = this@center.width
        val height = this@center.height

        x = (left + width / 2).toInt()
        y = (top + height / 2).toInt()
    }

    /**
     * This is called whenever a sensor event is received from invalidate().
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas?.apply {

            //get the center of the reticle
            val point = mReticle.center()

            //get the x and y rotation based on the sensor input
            val pitch = mOrientation?.get(1) ?: 0.0f
            val roll = mOrientation?.get(2) ?: 0.0f

            //convert the pitch and roll to degrees
            val adjustPitch = Math.toDegrees(pitch.toDouble())
            val adjustRoll = Math.toDegrees(roll.toDouble())

            //get the screen orientation
            val orientation = resources.configuration.orientation

            //adjust adjustments based on screen orientation
            val adjustX = point.x.toFloat() +
                    if (orientation == ORIENTATION_PORTRAIT) adjustRoll.toFloat()
                    else adjustPitch.toFloat()
            val adjustY = point.y.toFloat() +
                    if (orientation == ORIENTATION_PORTRAIT) adjustPitch.toFloat()
                    else adjustRoll.toFloat()

            //determine green or yellow paint based on if we are within the calib. thresh
            val paint = if (abs(adjustPitch) < mCalibrationThreshold && abs(adjustRoll) < mCalibrationThreshold) {
                mSuccessCounter++ //increment the success count whenever we are within calib. thresh
                mCalibrationSuccessPointPaint
            } else {
                onRecalibrate() //this will be called to reset the calibration
                mCalibrationPointPaint
            }

            if (!mCalibrated) { //only draw the points if we are not calibrated

                //draws a reticle point and a point on the calib. cross
                drawPoint(point.x.toFloat(), point.y.toFloat(), mReticleCenterPoint)
                drawPoint(adjustX, adjustY, mCalibrationCenterPoint)

                //next two drawLines draw the cross at the reticle and the cursor
                drawLine(adjustX-mCalibrationLineLength, adjustY,
                    adjustX+mCalibrationLineLength, adjustY, paint)

                drawLine(adjustX, adjustY-mCalibrationLineLength,
                    adjustX, adjustY+mCalibrationLineLength, paint)

                if (mSuccessCounter >= mSuccessThreshold) {
                    onCalibrated()
                }
            }
        }
    }

    /**
     * Called when an adjustment does not meet the calib. threshold.
     * Resets the views visibility and sets the calib. flag to false.
     */
    private fun onRecalibrate() {

        mSuccessCounter = 0

        mCalibrated = false

        mReticle.visibility = View.VISIBLE
    }

    /**
     * Does a small vibration and makes the view invisible.
     */
    private fun onCalibrated() {

        mCalibrated = true

        mVibrator.vibrate(VibrationEffect.createWaveform(longArrayOf(1L, 5L, 8L, 5L, 1L), -1))

        mReticle.visibility = View.INVISIBLE

        mSuccessCounter = 0
    }

    /**
     * Listens to sensors changes.
     * Currently this view only listens for accelerometer and magnetic field.
     * Both events are ran through a low pass filter to reduce noise.
     */
    override fun onSensorChanged(event: SensorEvent?) {

        if (event?.sensor != null) {
            if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
                mGravity = lowPassFilter(event.values.clone(), mGravity)
            } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
                mGeomagneticField =
                    lowPassFilter(event.values.clone(), mGeomagneticField)
            }

            //when we have both data start updating with onDraw after getting the orientation
            if (mGravity != null && mGeomagneticField != null) {
                val R = FloatArray(9)
                val I = FloatArray(9)
                if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagneticField)) {
                    mOrientation = FloatArray(3)
                    SensorManager.getOrientation(R, mOrientation)

                    invalidate()

                }
            }
        }
    }

    /**
     * Smooth two float arrays using a low pass filter.
     * The constant used when closer to 1 makes more of a jitter, closer to 0 is a floating result.
     */
    private fun lowPassFilter(input: FloatArray, output: FloatArray?): FloatArray =
        output?.mapIndexed { index, fl -> fl + 0.5f * (input[index] - fl) }?.toFloatArray() ?: input

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}