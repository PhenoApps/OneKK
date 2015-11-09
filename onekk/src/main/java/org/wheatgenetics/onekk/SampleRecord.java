package org.wheatgenetics.onekk;

public class SampleRecord {

    private int id;
    private String sampleId;
    private String photo;
    private String personId;
    private String date;
    private String seedCount;
    private String weight;
    private Double avgArea;
    private Double avgLength;
    private Double avgWidth;

    public SampleRecord() {
    }

    public SampleRecord(String sampleId, String photo,
                        String personId, String date, String seedCount, String weight, Double avgArea,
                        Double avgLength, Double avgWidth) {
        super();
        this.sampleId = sampleId;
        this.photo = photo;
        this.personId = personId;
        this.date = date;
        this.seedCount = seedCount;
        this.weight = weight;
        this.avgArea = avgArea;
        this.avgLength = avgLength;
        this.avgWidth = avgWidth;
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

    public Double getAvgArea() {
        // TODO Auto-generated method stub
        return avgArea;
    }

    public Double getAvgLength() {
        // TODO Auto-generated method stub
        return avgLength;
    }

    public Double getAvgWidth() {
        // TODO Auto-generated method stub
        return avgWidth;
    }

    public void setAvgArea(Double avgArea) {
        this.avgArea = avgArea;
    }

    public void setAvgLength(Double avgLength) {
        this.avgLength = avgLength;
    }

    public void setAvgWidth(Double avgWidth) {
        this.avgWidth = avgWidth;
    }

    @Override
    public String toString() {
        return sampleId + "," + photo + "," + personId + "," + date + "," + seedCount + ","
                + weight + "," + avgArea + "," + avgLength + "," + avgWidth;
    }

}
