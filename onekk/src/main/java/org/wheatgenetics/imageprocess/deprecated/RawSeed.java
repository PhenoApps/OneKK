package org.wheatgenetics.imageprocess.deprecated;

import org.opencv.core.CvType;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

/**
 * Created by sid on 2/22/18.
 */

/**
 * can be renamed to Seed if the MeasureSeeds class is used
 */
public class RawSeed {
    private double length;
    private double width;
    private double circ;
    private double area;
    private double perimeter;
    private Scalar color;
    private double lwr; // length to width ratio
    private Point centgrav = new Point(); // center of gravity
    private Point intLW = new Point(); //intersection of length and width vector
    private double ds; // distance between centgrav and intLW;
    private double tolerance = 0.2;
    private boolean isCanonical = false;
    private double seedPixelSize = 0;

    private MatOfPoint2f seedMat = new MatOfPoint2f();
    private MatOfPoint perm = new MatOfPoint();
    private MatOfPoint perm2 = new MatOfPoint();
    private RotatedRect rec = new RotatedRect();
    private RotatedRect elp = new RotatedRect();
    private Point[] ptsL = new Point[2];
    private Point[] ptsW = new Point[2];

    /**
     * Class to hold seed matrix array and descriptors
     */
    public RawSeed(MatOfPoint2f mat, MatOfPoint2f matHull) {
        seedMat = mat;

        mat.convertTo(perm, CvType.CV_32S);
        matHull.convertTo(perm2, CvType.CV_32S);

        rec = Imgproc.minAreaRect(mat);
        elp = Imgproc.fitEllipse(mat);
        circ = 4 * Math.PI * Imgproc.contourArea(mat) / Math.pow(Imgproc.arcLength(mat, true), 2); // calculate circularity
        area = Imgproc.contourArea(mat);
        perimeter = Imgproc.arcLength(mat, true);
        //color = Core.mean(image2.submat(roi),labels.submat(roi)); // TODO add switch for analyzing color
        this.calcCG();
        this.findMaxVec();
        //this.findIS();
    }

    /**
     * Getter classes
     */
    public double getLength() {
        return (length / 2) * seedPixelSize;
    }

    public double getWidth() {
        return (width / 2) * seedPixelSize;
    }

    public double getCirc() {
        return circ * seedPixelSize;
    }

    public double getArea() {
        return area * seedPixelSize * seedPixelSize;
    }

    public double getPerimeter() {
        return perimeter * seedPixelSize;
    }

    public Scalar getSeedColor() {
        return color;
    }

    /**
     * Find the center of gravity by averaging all points in the perimeter
     * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
     */
    private void calcCG() {
        double sumX = 0;
        double sumY = 0;
        Point[] permArray = perm2.toArray();
        for (Point aPermArray : permArray) {
            sumX += aPermArray.x;
            sumY += aPermArray.y;

        }
        centgrav.x = Math.round(sumX / permArray.length);
        centgrav.y = Math.round(sumY / permArray.length);
    }

    /**
     * Find the end-point coordinates of the maxium vector in the seed
     * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
     */
    private void findMaxVec() {
        Point[] permArray = perm2.toArray();

        for (int i = 0; i < permArray.length; i++) {
            for (int j = i; j < permArray.length; j++) {
                Point p1 = permArray[i];
                Point p2 = permArray[j];
                double l = Math.sqrt(Math.pow(p1.x - p2.x, 2) + Math.pow(p1.y - p2.y, 2));
                if (l > length) {
                    length = l;
                    ptsL[0] = p1;
                    ptsL[1] = p2;
                }
            }
        }

        permArray = perm.toArray();

        double slopeL = ((ptsL[1].y - ptsL[0].y) / (ptsL[1].x - ptsL[0].x));  // TODO not sure this works for infinite slope

        //TODO use perm2 to calculate width

        for (Point aPermArray : permArray) {
            double d = 1;
            Point p2 = aPermArray;
            for (Point aPermArray1 : permArray) {
                double s = slopeL * ((aPermArray.y - aPermArray1.y) / (aPermArray.x - aPermArray1.x));
                if (Math.abs(s + 1) < d) {
                    d = Math.abs(s + 1);
                    p2 = aPermArray1;
                }
            }

            double w = Math.sqrt(Math.pow(aPermArray.x - p2.x, 2) + Math.pow(aPermArray.y - p2.y, 2));
            if (w > width) {
                width = w;
                ptsW[0] = aPermArray;
                ptsW[1] = p2;
            }
        }

        lwr = length / width;
    }

    /**
     * Find the intersection of max length and max width vector
     * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
     */
    private void findIS() {
        if (ptsW[0] == null) {
            return;
        }
        ; // exit method if width is null
        double S1 = 0.5 * ((ptsW[0].x - ptsW[1].x) * (ptsL[0].y - ptsW[1].y) - (ptsW[0].y - ptsW[1].y) * (ptsL[0].x - ptsW[1].x));
        double S2 = 0.5 * ((ptsW[0].x - ptsW[1].x) * (ptsW[1].y - ptsL[1].y) - (ptsW[0].y - ptsW[1].y) * (ptsW[1].x - ptsL[1].x));
        intLW.x = ptsL[0].x + S1 * (ptsL[1].x - ptsL[0].x) / (S1 + S2);
        intLW.y = ptsL[0].y + S1 * (ptsL[1].y - ptsL[0].y) / (S1 + S2);

        ds = Math.sqrt(Math.pow(intLW.x - centgrav.x, 2) + Math.pow(intLW.y - centgrav.y, 2));

    }

    /**
     * Returns the maximum vector length
     * As described by Tanabata, T., T. Shibaya, K. Hori, K. Ebana and M. Yano (2012) "SmartGrain: high-throughput phenotyping software for measuring seed shape through image analysis." Plant physiology 160(4): 1871-1880.
     *
     * @return double giving the maximum vector length
     */
    private double getMaxVec() {
        return Math.sqrt(Math.pow(ptsL[0].x - ptsL[1].x, 2) + Math.pow(ptsL[0].y - ptsL[1].y, 2));
    }

    /**
     * Runs multiple checks to determine if the shape blob represents a canonical seed shape
     */
    private boolean checkCanonical() {
        double minSize = 30;
        return this.length >= minSize && this.checkCirc() && this.checkElp();
    }

    /**
     * Checks and expected circularity value is within the expected circularity range
     */
    private boolean checkCirc() {
        double minCirc = 0.6;
        return minCirc < circ;
    }

    /**
     * Checks the object is roughly an eliptical shape.  Will filter blobs of two or more seeds
     * Not sure this is working correctly due to approximation formula for circumference of ellipse.
     */
    private boolean checkElp() {
        double a = elp.boundingRect().height / 2;
        double b = elp.boundingRect().width / 2;
        double c = 2 * Math.PI * Math.sqrt((Math.pow(a, 2) + Math.pow(b, 2)) / 2); // TODO this is the approximation formula for circumference of an ellipse - FIGURE OUT IF IT IS WORKING
        double p = Imgproc.arcLength(seedMat, true);

        return p < 1.1 * c;
    }

    /**
     * Checks and expected length to width ratio is within a tolerance limit // DEFAULT SET TO 30%
     */
    private boolean checkL2W() {
        double expLWR = 1.2;
        return lwr > expLWR * (1 - tolerance) & lwr < expLWR * (1 + tolerance);
    }

    public void setTolerance(double t) {
        tolerance = t;
    }

    public double getSeedPixelSize() {
        return seedPixelSize;
    }

    public void setSeedPixelSize(double seedPixelSize) {
        this.seedPixelSize = seedPixelSize;
    }

    public MatOfPoint getPerm() {
        return perm;
    }

    public Point[] getPtsL() {
        return ptsL;
    }

    public Point[] getPtsW() {
        return ptsW;
    }

    public Point getCentgrav() {
        return centgrav;
    }
}