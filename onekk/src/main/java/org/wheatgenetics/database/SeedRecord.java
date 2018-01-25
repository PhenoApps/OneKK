package org.wheatgenetics.database;

public class SeedRecord {

    private int id;
    private String sampleId;
    private double length;
    private double width;
    private double circularity;
    private String color;
    private double area;
    private String weight;

    public SeedRecord() {
    }

    public SeedRecord(String sampleId, double length, double width, double circularity, double area, String weight, String color) {
        super();
        this.sampleId = sampleId;
        this.length = length;
        this.width = width;
        this.circularity = circularity;
        this.color = color;
        this.area = area;
        this.weight = weight;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getSampleId() {
        return sampleId;
    }

    public void setSampleId(String sampleId) {
        this.sampleId = sampleId;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getWidth() {
        return width;
    }

    public void setWidth(double width) {
        this.width = width;
    }

    public void setCircularity(double circularity) {
        this.circularity = circularity;
    }

    public double getCircularity() {
        return circularity;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public double getArea() {
        return area;
    }

    public void setArea(double area) {
        this.area = area;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    @Override
    public String toString() {
        return sampleId + "," + length + "," + width + "," + circularity + "," + color + "," + area + "," + weight;
    }
}
