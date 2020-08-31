package org.wheatgenetics.imageprocess.renderscript

import android.content.Context
import android.graphics.Bitmap
import android.renderscript.*
import android.util.Log
import androidx.core.graphics.scale

import org.wheatgenetics.imageprocess.ScriptC_singlesource

//renderscript support library
//import android.support.v8.renderscript.*

class ExampleRenderScript(val context: Context) {

    private val rs = RenderScript.create(context)

//    fun yuvToRgb(bitmap: Bitmap?, context: Context?): Bitmap? {
//
//        //Create renderscript
//        val rs = RenderScript.create(context)
//
//        //Create allocation from Bitmap
//        val allocation = Allocation.createFromBitmap(rs, bitmap)
//
//        //Create allocation with the same type
//        val out = Allocation.createTyped(rs, allocation.type)
//
//        //Create script
//        val script = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs))
//
//        //Set input for script
//        script.setInput(allocation)
//
//        script.forEach(out)
//
//        //Copy script result into bitmap
//        out.copyTo(bitmap)
//
//        //Destroy everything to free memory
////        allocation.destroy()
////        blurredAllocation.destroy()
////        blurScript.destroy()
////        t.destroy()
////        rs.destroy()
//        return bitmap
//    }
//
//    fun histogramEqualization(image: Bitmap): Bitmap? {
//        //Get image size
//        val width = image.width
//        val height = image.height
//
//        //Create new bitmap
//        val res = image.copy(image.config, true)
//
//        //Create renderscript
//        val rs = RenderScript.create(context)
//
//        //Create allocation from Bitmap
//        val allocationA = Allocation.createFromBitmap(rs, res)
//
//        //Create allocation with same type
//        val allocationB = Allocation.createTyped(rs, allocationA.type)
//
//        //Create script from rs file.
//        val histEqScript = ScriptC_histEq(rs)
//
//        //Set size in script
//        histEqScript.set_size(width * height)
//
//        //Call the first kernel.
//        histEqScript.forEach_root(allocationA, allocationB)
//
//        //Call the rs method to compute the remap array
//        histEqScript.invoke_createRemapArray()
//
//        //Call the second kernel
//        histEqScript.forEach_remaptoRGB(allocationB, allocationA)
//
//        //Copy script result into bitmap
//        allocationA.copyTo(res)
//
//        return res
//    }
//
//    fun mandelbrotBitmap(bitmap: Bitmap?, context: Context?): Bitmap? {
//        //Create renderscript
//        val rs = RenderScript.create(context)
//
//        //Create allocation from Bitmap
//        val allocation = Allocation.createFromBitmap(rs, bitmap)
//        val t: Type = allocation.type
//
//        //Create allocation with the same type
//        val out = Allocation.createTyped(rs, t)
//
//        val kernel = floatArrayOf(
//                0.1f, -1f, 0.1f,
//                -1f, 4f, -1f,
//                0.1f, -1f, 0.1f
//        )
//        //Create script
//        val script = ScriptC_mandelbrot(rs)
//        //Set blur radius (maximum 25.0)
//        //script.setCoefficients(kernel)
//        //Set input for script
//        //script.setInput(allocation)
//        //Call script for output allocation
//        script.forEach_root(allocation)
//        //script.setInput(out)
//        //script.forEach(allocation)
//
//        //Copy script result into bitmap
//        allocation.copyTo(bitmap)
//
//        return bitmap
//    }
//
//    fun convolveLaplaceBitmap(bitmap: Bitmap?): Bitmap? {
//
//        //Create allocation from Bitmap
//        val allocation = Allocation.createFromBitmap(rs, bitmap)
//        val t: Type = allocation.type
//
//        //Create allocation with the same type
//        val out = Allocation.createTyped(rs, t)
//
//        val kernel = floatArrayOf(
//                0.1111111111111111f, -1f, 0.1111111111111f,
//                -1f, 4f, -1f,
//                0.1111111111111111f, -1f, 0.1111111111111f
//        )
//
////        val kernel2 = floatArrayOf(
////                -1f, -1f, -1f,
////                -1f, 8f, -1f,
////                -1f, -1f, -1f
////        )
//        //Create script
//        val script = ScriptIntrinsicConvolve3x3.create(rs, Element.U8_4(rs))
//        //Set blur radius (maximum 25.0)
//        script.setCoefficients(kernel)
//        //Set input for script
//        script.setInput(allocation)
//        //Call script for output allocation
//        script.forEach(out)
//
//        //Copy script result into bitmap
//        out.copyTo(bitmap)
//
//        return bitmap
//    }
//
//    fun blur(bitmap: Bitmap?, radius: Float): Bitmap? {
//
//        //Create allocation from Bitmap
//        val allocation = Allocation.createFromBitmap(rs, bitmap)
//        val t: Type = allocation.type
//
//        //Create allocation with the same type
//        val blurredAllocation = Allocation.createTyped(rs, t)
//
//        //Create script
//        val blurScript: ScriptIntrinsicBlur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs))
//        //Set blur radius (maximum 25.0)
//        blurScript.setRadius(radius)
//        //Set input for script
//        blurScript.setInput(allocation)
//        //Call script for output allocation
//        blurScript.forEach(blurredAllocation)
//
//        //Copy script result into bitmap
//        blurredAllocation.copyTo(bitmap)
//
//        //Destroy everything to free memory
////        allocation.destroy()
////        blurredAllocation.destroy()
////        blurScript.destroy()
////        t.destroy()
////        rs.destroy()
//        return bitmap
//    }
//
//    fun resize(src: Bitmap) {
//
//        Log.d("Source:", src.config.name)
//
//        //val output = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)
//
//        val renderScript: RenderScript = RenderScript.create(context)
//
//        val script = ScriptIntrinsicResize.create(renderScript)
//
//        val inputAllocation = Allocation.createFromBitmap(renderScript, src)
//
////            val inputAllocation: Allocation = Allocation.createFromBitmapResource(
////                    rs,
////                    resources,
////                    R.drawable.image
////            )
//
//        val outputAllocation = Allocation.createFromBitmap(renderScript, Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888))
////
////        val outputAllocation: Allocation = Allocation.createTyped(
////                renderScript,
////                inputAllocation.type,
////                Allocation.USAGE_SCRIPT or Allocation.USAGE_IO_OUTPUT
////        )
//
//        script.setInput(inputAllocation)
//
//        script.forEach_bicubic(outputAllocation)
//
//        //return src.copy(src.config, true).also {
//
//        outputAllocation.copyTo(src.scale(1080, 1920))
//
//        //}
//
//    }
//
//    fun testScript(src: Bitmap, context: Context): Bitmap {
//
//        val renderScript: RenderScript = RenderScript.create(context)
//
//        val script = ScriptC_singlesource(renderScript)
//
//        val inputAllocation = Allocation.createFromBitmap(renderScript, src)
//
////            val inputAllocation: Allocation = Allocation.createFromBitmapResource(
////                    rs,
////                    resources,
////                    R.drawable.image
////            )
//
//      //  val outputAllocation = Allocation.createFromBitmap(renderScript, dst)
//
//        val outputAllocation: Allocation = Allocation.createTyped(
//                renderScript,
//                inputAllocation.type,
//                Allocation.USAGE_SCRIPT or Allocation.USAGE_IO_OUTPUT
//        )
//
//        script.invoke_process(inputAllocation, outputAllocation)
//
//        return src.copy(src.config, true).also {
//
//            outputAllocation.copyTo(it)
//
//        }
//
//    }

}