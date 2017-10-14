package org.wheatgenetics.imageprocess.watershed;

/**
 * Created by sid on 9/28/17.
 */

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class EDM {
    public static final int BYTE_OVERWRITE = 0;
    public static final int FLOAT = 3;
    private static int outputType = BYTE_OVERWRITE;
    private static final String TAG = "Arunachala";
    private static final int NO_POINT = -1;
    private int flags = 1;
    private static Context context;
    boolean background255 = true;
    int white = -1;
    int black = -16777216;
    String path;
    private static double MAXFINDER_TOLERANCE = 1.2;
    public static String MyDate;
    MaximumFinder maxFinder = new MaximumFinder();
    double Threshold;

    public EDM(){}

    public void setThreshold(double threshold){
        //Threshold = threshold;
        MAXFINDER_TOLERANCE=threshold;
    }

    public EDM(Context c){
        context = c;
    }

    public void setup(Bitmap bitmap){
        //getPixelValue(bitmap);
        int[] demo = new int[bitmap.getHeight()*bitmap.getWidth()];
        int a=demo.length;
        //print("Demo length is:"+String.valueOf(a));
/*
        if(checkBinary(bitmap)){
            print("bitmap is Binary");
        }else{
            print("bitmap is not binary");
            Toast.makeText(context, "Binary image with black and white pixels required!", Toast.LENGTH_LONG).show();
        }

        if(checkLut(bitmap)){
            print("bitmap is inverted");
        }
        else
        {
            print("bitmap is not inverted");
            Toast.makeText(context, "Binary image with inverted black and white pixels required!", Toast.LENGTH_LONG).show();
        }
        */
        run(bitmap);
    }

    public void run(Bitmap bitmap) {
        //if(interrupted) return;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float a, b;
        a = Float.MIN_VALUE;
        b = Float.MIN_VALUE;
        int backgroundValue = black;
        float[][] floatedm = makeFloatEDM(bitmap, backgroundValue, false);

        // int[][] maxIp = maxFinder.findMaxima(floatedm, width, height, MAXFINDER_TOLERANCE,
        //         -808080.0, MaximumFinder.SEGMENTED, false, true);

        int[][] maxIp = maxFinder.findMaxima(floatedm, width, height, MAXFINDER_TOLERANCE,
                Threshold, MaximumFinder.SEGMENTED, false, true);

        int[] pixels = new int[width * height];

        for (int i = 0, k = 0; i < height; i++) {
            for (int j = 0; j < width; k++, j++) {
                if(maxIp[j][i]==0)
                    pixels[k]=black;
                else
                    pixels[k] = maxIp[j][i];
            }
        }
        bitmap.setPixels(pixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        //mysave(bitmap);
    }

    public float[][] makeFloatEDM(Bitmap bitmap, int backgroundValue, boolean edgesAreBackground){
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float[][] fp = new float[width][height];
        int[] bPixels = new int[height*width];
        bitmap.getPixels(bPixels, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        //testing
        int test=0;
        int count= 0;

        for(int x=0; x<width*height; x++){
            if(bPixels[x]!=backgroundValue){
                //    if(test ==0)
                //  {
                //          print(" x is: "+x);
                //    test++;
                //}count++;
                fp[x%width][x/width] = Float.MAX_VALUE;
            }
        }
        // print("count is : "+count);
        // saveImage(bitmap);

        int[][] pointBufs = new int[2][width];
        int yDist = Integer.MAX_VALUE;
        for(int x=0; x<width; x++){
            pointBufs[0][x] = NO_POINT;
            pointBufs[1][x] = NO_POINT;
        }
        for(int y=0; y<0; y++){
            if(edgesAreBackground) yDist = y+1;
            edmLine(bPixels, fp, pointBufs, width, y*width, y, backgroundValue, yDist);
        }
        for(int x=0; x<width; x++){
            pointBufs[0][x] = NO_POINT;
            pointBufs[1][x] = NO_POINT;
        }
        for(int y=height-1; y>=0; y--){
            if(edgesAreBackground) yDist = height - y;
            edmLine(bPixels, fp, pointBufs, width, y*width, y, backgroundValue, yDist);

        }
        for(int i=0; i<width; i++){
            for(int j=0; j<height; j++){
                fp[i][j] = (float)Math.sqrt(fp[i][j]);
            }
        }
        return fp;
    }

    public File mysave(Bitmap ImageToSave) {
        MyDate = getCurrentDateAndTime();
        FileOutputStream outStream = null;
        File outFile = null;
        try {
            File MyDir = Environment.getExternalStorageDirectory();
            File dir = new File(MyDir.getAbsolutePath() + "/Watershed/WatershedImages");
            dir.mkdirs();

            String fileName = "bitmap" + MyDate + ".jpg";
            outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            path = outFile.getAbsolutePath();
            print("File saved at " + path);
            //refreshGallery(outFile);
            //return outFile;

        } catch (FileNotFoundException e) {
            print("FileNotFound exception");
            e.printStackTrace();
        } catch (IOException e) {
            print("IOException");
            e.printStackTrace();
        } finally {

        }return outFile;
    }

    private void edmLine(int[] bPixlels, float[][] fp, int[][] pointBufs, int width, int offset, int y,
                         int backgroundValue, int yDist){
        int[] points = pointBufs[0];
        int pPrev = NO_POINT;
        int pDiag = NO_POINT;
        int pNextDiag;
        boolean edgesAreBackground = yDist!= Integer.MAX_VALUE;
        int distSqr = Integer.MAX_VALUE;
        for(int x=0; x<width; x++, offset++){
            pNextDiag = points[x];
            if(bPixlels[offset] == backgroundValue){
                points[x] = x | y<<16;
            }else{
                if(edgesAreBackground){
                    distSqr = (x+1 < yDist) ? (x+1)*(x+1) : yDist*yDist;
                }
                float dist2 = minDist2(points, pPrev, pDiag, x, y, distSqr);
                if(fp[offset%width][offset/width] > dist2){
                    fp[offset%width][offset/width] = dist2;
                }
            }
            pPrev = points[x];
            pDiag = pNextDiag;
        }
        offset--;
        points= pointBufs[1];
        pPrev = NO_POINT;
        pDiag = NO_POINT;
        for(int x=width-1; x>=0; x--, offset--){
            pNextDiag = points[x];
            if(bPixlels[offset] == backgroundValue){
                points[x] = x | y<<16;
            }else{
                if(edgesAreBackground){
                    distSqr = (x+1 < yDist) ? (x+1)*(x+1) : yDist*yDist;
                }
                float dist2 = minDist2(points, pPrev, pDiag, x, y, distSqr);
                if(fp[offset%width][offset/width] > dist2){
                    fp[offset%width][offset/width] = dist2;
                }
            }
            pPrev = points[x];
            pDiag = pNextDiag;
        }
    }

    private float minDist2(int[] points, int pPrev, int pDiag, int x, int y, int distSqr){
        int p0 = points[x];              // the nearest background point for the same x in the previous line
        int nearestPoint = p0;
        if (p0 != NO_POINT) {
            int x0 = p0& 0xffff; int y0 = (p0>>16)&0xffff;
            int dist1Sqr = (x-x0)*(x-x0)+(y-y0)*(y-y0);
            if (dist1Sqr < distSqr)
                distSqr = dist1Sqr;
        }
        if (pDiag!=p0 && pDiag!=NO_POINT) {
            int x1 = pDiag&0xffff; int y1 = (pDiag>>16)&0xffff;
            int dist1Sqr = (x-x1)*(x-x1)+(y-y1)*(y-y1);
            if (dist1Sqr < distSqr) {
                nearestPoint = pDiag;
                distSqr = dist1Sqr;
            }
        }
        if (pPrev!=pDiag && pPrev!=NO_POINT) {
            int x1 = pPrev& 0xffff; int y1 = (pPrev>>16)&0xffff;
            int dist1Sqr = (x-x1)*(x-x1)+(y-y1)*(y-y1);
            if (dist1Sqr < distSqr) {
                nearestPoint = pPrev;
                distSqr = dist1Sqr;
            }
        }
        points[x] = nearestPoint;
        return (float)distSqr;
    }

    public void toWatershed(Bitmap bitmap){
        int[] pixels = new int[bitmap.getHeight()*bitmap.getWidth()];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(),0,0,bitmap.getWidth(), bitmap.getHeight());
        return;
    }

    //faster method to check if the bitmap is binary or not
    public Boolean checkBinary(Bitmap bitmap){
        int[] pixels = new int[bitmap.getHeight()*bitmap.getWidth()];
        int a=pixels.length;
        print(String.valueOf(a));
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0,0,bitmap.getWidth(), bitmap.getHeight());
        for(int i=0; i < (bitmap.getWidth()*bitmap.getHeight()); i++){
            if(!((pixels[i] == -16777216) ||(pixels[i] == -1))){
                print("value at i: "+i);
                return false;
            }
        }return true;
    }

    public Boolean checkLut(Bitmap bitmap){
        for(int i=0; i<bitmap.getWidth(); i++){
            for(int j=0; j<bitmap.getHeight(); j++){
                int pixelValue = bitmap.getPixel(i,j);
                int red = Color.red(pixelValue);
                int blue = Color.blue(pixelValue);
                int green =  Color.green(pixelValue);
                if((red == 255) && (green == 255) && (blue ==255)){
                    if(pixelValue == -1){
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
                else if((red == 0) && (green == 0) && (blue ==0)){
                    if(pixelValue == -16777216){
                        return true;
                    }
                    else
                    {
                        return false;
                    }
                }
            }
        }return false;
    }

    public void toEDM(Bitmap bitmap){

    }

    public static void setOutputType(int type) {
        if (type<BYTE_OVERWRITE || type>FLOAT)
            throw new IllegalArgumentException("Invalid type: "+type);
        outputType = type;
    }

    /** Returns the current output type (BYTE_OVERWRITE, BYTE, SHORT or FLOAT) */
    public static int getOutputType() {
        return outputType;
    }

    public File saveImage(Bitmap ImageToSave) {
        MyDate = getCurrentDateAndTime();
        FileOutputStream outStream = null;
        File outFile = null;
        try {
            File MyDir = Environment.getExternalStorageDirectory();
            File dir = new File(MyDir.getAbsolutePath() + "/Watershed/");
            dir.mkdirs();

            String fileName = "my.jpg";
            outFile = new File(dir, fileName);

            outStream = new FileOutputStream(outFile);
            ImageToSave.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            path = outFile.getAbsolutePath();
            print("File saved at " + path);
            //refreshGallery(outFile);
            //return outFile;

        } catch (FileNotFoundException e) {
            print("FileNotFound exception");
            e.printStackTrace();
        } catch (IOException e) {
            print("IOException");
            e.printStackTrace();
        } finally {

        }return outFile;
    }


    private String getCurrentDateAndTime() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
        String formattedDate = df.format(c.getTime());
        return formattedDate;
    }
    public void print(String s){
        Log.d(TAG, s);
    }



    //testing methods//////////////////////////////////////////////////////////////////////////////////////////////
    //slower method to check if the bitmap is binary or not
    public Boolean checkIfBinary(Bitmap bitmap){
        for(int i= 0; i<bitmap.getWidth(); i++){
            for(int j=0; j<bitmap.getHeight();j++){
                int pixelValue = bitmap.getPixel(i,j);
                if(!((pixelValue==-1)||(pixelValue==-16777216))){
                    return false;
                }
            }
        }
        return true;
    }

    public void getPixelValue(Bitmap bitmap){
        for(int i=0; i<bitmap.getWidth(); i++){
            for(int j=0; j<bitmap.getHeight(); j++){
                int pixelValue = bitmap.getPixel(i,j);
                int red = Color.red(pixelValue);
                int green = Color.green(pixelValue);
                int blue = Color.blue(pixelValue);
                print("Pixel color value at coordinate ["+i+","+j+"] is: "+String.valueOf(pixelValue)+
                        " and Red: "+String.valueOf(red)+", " +
                        "Blue: "+String.valueOf(blue)+", " +
                        "Green: "+String.valueOf(green));
            }
        }
    }
    public void getWhitePixelValue(Bitmap bitmap){
        for(int i=0; i<bitmap.getWidth(); i++){
            for(int j=0; j<bitmap.getHeight(); j++){
                int pixelValue = bitmap.getPixel(i,j);
                if(pixelValue == -1){
                    int red = Color.red(pixelValue);
                    int green = Color.green(pixelValue);
                    int blue = Color.blue(pixelValue);
                    print("Pixel color value at coordinate ["+i+","+j+"] is: "+String.valueOf(pixelValue)+
                            " and Red: "+String.valueOf(red)+", " +
                            "Blue: "+String.valueOf(blue)+", " +
                            "Green: "+String.valueOf(green));
                }
            }
        }
    }
}
