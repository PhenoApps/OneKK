package org.wheatgenetics.imageprocess.watershed;

/**
 * Created by sid on 9/28/17.
 */

public class CPoint implements Comparable<CPoint>{

    private int x;
    private int y;
    private int xProd;
    public double slope;

    public CPoint(int xVal, int yVal, double xProdVal, double slope) {
        super();
        this.x = xVal;
        this.y = yVal;
        this.xProd = (int) Math.round(xProdVal);
        this.slope = slope;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getXProd() {
        return xProd;
    }

    public void setXProd(double xProdVal) {
        this.xProd = (int) Math.round(xProdVal);
    }

    public void setXProd(int xProdVal) {
        this.xProd = xProdVal;
    }

    @Override
    public int compareTo(CPoint compareValue) {

        double compareXProd = ((CPoint) compareValue).getXProd();

        //descending order
        return (int) Math.round(compareXProd - this.xProd);
    }
}