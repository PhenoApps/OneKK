package org.wheatgenetics.imageprocess.Seed;

import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

/**
 * Created by sid on 4/2/18.
 */

public class Seed {

    private double x = 0;
    private double y = 0;
    private double area = 0;
    private double perimeter = 0;
    private Scalar seedColor = null;
    private double pixelMetric = 0;
    private Rect boundingRect = null;
    private MatOfPoint matOfPoint = null;

    public Seed(double x, double y, double area, double perimeter, Scalar seedColor, double pixelMetric, Rect boundingRect, MatOfPoint matOfPoint) {
        this.x = x;
        this.y = y;
        this.area = area * pixelMetric * pixelMetric;
        this.perimeter = perimeter * pixelMetric;
        this.seedColor = seedColor;
        this.pixelMetric = pixelMetric;
        this.boundingRect = boundingRect;
        this.matOfPoint = matOfPoint;
    }

    @Override
    public String toString(){
        return new Point(x,y).toString() + " " + area + " " + perimeter + " " + seedColor.toString();
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getArea() {
        return area;
    }

    public double getPerimeter() {
        return perimeter;
    }

    public Scalar getSeedColor() {
        return seedColor;
    }

    public Rect getBoundingRect() {
        return boundingRect;
    }

    public double getWidth(){
        return boundingRect.width * pixelMetric;
    }

    public double getHeight(){
        return boundingRect.height * pixelMetric;
    }

    public MatOfPoint getMatOfPoint() {
        return matOfPoint;
    }
}