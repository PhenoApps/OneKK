package org.wheatgenetics.imageprocess.Seed;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;

/**
 * Created by sid on 4/12/18.
 */

public class MSeeds {

    private ArrayList<Seed> mSeedsArrayList;
    private double pixelMetric;
    private Mat mSeedsProcessedMat;

    public MSeeds(double pixelMetric, Mat processedMat){
        this.pixelMetric = pixelMetric;
        this.mSeedsProcessedMat = processedMat;
    }

    public void process(ArrayList<Seed> seedArrayList){
        int[] textSize = new int[1];
        mSeedsArrayList = new ArrayList<>();

        for(Seed seed : seedArrayList) {

            MatOfPoint matOfPoint = seed.getMatOfPoint();
            final Point[] contourPoints = matOfPoint.toArray();
            final MatOfPoint2f contour = new MatOfPoint2f(contourPoints);

            Rect boundingRect = Imgproc.boundingRect(matOfPoint);

            /* determine the min area rectangle to estimate the size of seed */
            RotatedRect rotatedRect = Imgproc.minAreaRect(contour);
            Mat boxPoints = new Mat();
            Imgproc.boxPoints(rotatedRect, boxPoints);

            Point midpoints[] = new Point[4];
            Point points[] = new Point[4];
            rotatedRect.points(points);

            /* draw the rotated rect points and compute the midpoints of the sides */
            for (int v = 0; v < 4; v++) {
                Imgproc.line(mSeedsProcessedMat, points[v], points[(v + 1) % 4], new Scalar(v * 50, v * 50, 255), 5);
                midpoints[v] = new Point((points[v].x + points[(v + 1) % 4].x) / 2, (points[v].y + points[(v + 1) % 4].y) / 2);
            }

            /* calculate the euclidean distance between the midpoints of the sides*/

            /* euclidean distance to estimate the length of the seed */
            double midx = Math.pow((midpoints[0].x - midpoints[2].x), 2);
            double midy = Math.pow((midpoints[0].y - midpoints[2].y), 2);
            double midDistance1 = Math.sqrt(midx + midy);

            /* euclidean distance to estimate the width of the seed */
            midx = Math.pow((midpoints[1].x - midpoints[3].x), 2);
            midy = Math.pow((midpoints[1].y - midpoints[3].y), 2);
            double midDistance2 = Math.sqrt(midx + midy);

            String strWidth = String.format("%.2f", Math.min(midDistance1, midDistance2) * pixelMetric);

            String strHeight = String.format("%.2f", Math.max(midDistance1, midDistance2) * pixelMetric);

            /* define a rectangle with the length and width as a custom bounding box */
            Rect customBoundingBox = new Rect((int) points[0].x, (int) points[0].y, (int) Math.min(midDistance1, midDistance2), (int) Math.max(midDistance1, midDistance2));

            Imgproc.putText(mSeedsProcessedMat, strWidth, new Point(midpoints[0].x - (textSize[0] * 5), midpoints[0].y - (textSize[0] * 5)), Core.FONT_HERSHEY_COMPLEX, 1.0, new Scalar(0, 123, 255), 2);
            Imgproc.putText(mSeedsProcessedMat, strHeight, new Point(midpoints[1].x + (textSize[0] * 5), midpoints[1].y + (textSize[0] * 5)), Core.FONT_HERSHEY_COMPLEX, 1.0, new Scalar(0, 255, 255), 2);

            seed.setBoundingRect(customBoundingBox);
            seed.setSeedColor(seedColor(mSeedsProcessedMat,boundingRect));
            seed.setPixelMetric(pixelMetric);
            mSeedsArrayList.add(seed);
        }
    }

    private Scalar seedColor(Mat initialMat, Rect boundingRect){
        Scalar mBlobColorHsv;

        Mat touchedRegionRgba = initialMat.submat(boundingRect);

        Mat touchedRegionHsv = new Mat();
        Imgproc.cvtColor(touchedRegionRgba, touchedRegionHsv, Imgproc.COLOR_RGB2HSV);

        mBlobColorHsv = Core.sumElems(touchedRegionHsv);
        int pointCount = boundingRect.width * boundingRect.height;
        for (int i = 0; i < mBlobColorHsv.val.length; i++)
            mBlobColorHsv.val[i] /= pointCount;

        Mat pointMatRgba = new Mat();
        Mat pointMatHsv = new Mat(1, 1, CvType.CV_8UC3, mBlobColorHsv);
        Imgproc.cvtColor(pointMatHsv, pointMatRgba, Imgproc.COLOR_HSV2RGB, 4);

        return new Scalar(pointMatRgba.get(0, 0));
    }

    public ArrayList<Seed> getmSeedsArrayList() {
        return mSeedsArrayList;
    }

    public Mat getmSeedsProcessedMat() {
        return mSeedsProcessedMat;
    }
}
