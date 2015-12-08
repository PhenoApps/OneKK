package org.wheatgenetics.onekk;

public class SampleRecord {

    private int id;
    private String sampleId;
    private String photo;
    private String personId;
    private String date;
    private String seedCount;
    private String weight;
    private Double areaAvg;
    private Double areaVar;
    private Double areaCV;
    private Double lengthAvg;
    private Double lengthVar;
    private Double lengthCV;
    private Double widthAvg;
    private Double widthVar;
    private Double widthCV;

    public SampleRecord() {
    }

    public SampleRecord(String sampleId, String photo,
                        String personId, String date, String seedCount, String weight, Double avgLength,
                        Double varLength, Double cvLength, Double avgWidth, Double varWidth, Double cvWidth,
                        Double avgArea, Double varArea, Double cvArea) {
        super();
        this.sampleId = sampleId;
        this.photo = photo;
        this.personId = personId;
        this.date = date;
        this.seedCount = seedCount;
        this.weight = weight;
        this.lengthAvg = avgLength;
        this.lengthVar = varLength;
        this.lengthCV = cvLength;
        this.widthAvg = avgWidth;
        this.widthVar = varWidth;
        this.widthCV = cvWidth;
        this.areaAvg = avgArea;
        this.areaVar = varArea;
        this.areaCV = cvArea;
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

    public void setSampleId(String sample) {
        this.sampleId = sample;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }

    public String getPersonId() {
        return personId;
    }

    public void setPersonId(String person) {
        this.personId = person;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getDate() {
        return date;
    }

    public String getSeedCount() {
        return seedCount;
    }

    public void setSeedCount(String seedCount) {
        this.seedCount = seedCount;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }


    public Double getLengthAvg() { return lengthAvg; }

    public void setLengthAvg(Double avgLength) {
        this.lengthAvg = avgLength;
    }

    public Double getLengthVar() {
        return lengthVar;
    }

    public void setLengthVar(Double varLength) {
        this.lengthVar = varLength;
    }

    public Double getLengthCV() {
        return lengthCV;
    }

    public void setLengthCV(Double cvLength) {
        this.lengthCV = cvLength;
    }


    public Double getWidthAvg() {
        return widthAvg;
    }

    public void setWidthAvg(Double avgWidth) {
        this.widthAvg = avgWidth;
    }

    public Double getWidthVar() {
        return widthVar;
    }

    public void setWidthVar(Double varWidth) {
        this.widthVar = varWidth;
    }

    public Double getWidthCV() {
        return widthCV;
    }

    public void setWidthCV(Double cvWidth) {
        this.widthCV = cvWidth;
    }


    public Double getAreaAvg() {
        return areaAvg;
    }

    public void setAreaAvg(Double avgArea) {
        this.areaAvg = avgArea;
    }

    public Double getAreaVar() {
        return areaVar;
    }

    public void setAreaVar(Double varArea) {
        this.areaVar = varArea;
    }

    public Double getAreaCV() {
        return areaCV;
    }

    public void setAreaCV(Double cvArea) {
        this.areaCV = cvArea;
    }


    @Override
    public String toString() {
        return sampleId + "," + photo + "," + personId + "," + date + "," + seedCount + ","
                + weight + "," + areaAvg + "," + lengthAvg + "," + widthAvg;
    }

}
