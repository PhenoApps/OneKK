package org.wheatgenetics.database;

public class CoinRecord {

    private int id;
    private String country;
    private String currency;
    private String value;
    private String diameter;

    public CoinRecord() {
    }

    public CoinRecord(String country, String currency, String value, String diameter) {
        this.country = country;
        this.currency = currency;
        this.value = value;
        this.diameter = diameter;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getValue() {
        return value;
    }

    public Integer getIntValue() {
        return Integer.parseInt(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDiameter() {
        return diameter;
    }

    public Double getDoubleDiameter() {
        try{
            return Double.parseDouble(diameter);
        }
        catch (Exception ex){
         return null;
        }
    }

    public void setDiameter(String diameter) {
        this.diameter = diameter;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return country + "," + currency + "," + value + "," + diameter;
    }
}
