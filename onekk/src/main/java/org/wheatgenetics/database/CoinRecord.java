package org.wheatgenetics.database;

public class CoinRecord {

    private int id;
    private String country;
    private String primary_currency, secondary_currency;
    private String value, nominal, diameter, name;

    public CoinRecord() {
    }

    public CoinRecord(String country, String primary_currency, String value, String secondary_currency,String nominal, String diameter, String name) {
        this.country = country;
        this.primary_currency = primary_currency;
        this.value = value;
        this.secondary_currency = secondary_currency;
        this.nominal = nominal;
        this.diameter = diameter;
        this.name = name;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getPrimary_currency() {
        return primary_currency;
    }

    public void setPrimary_currency(String primary_currency) {
        this.primary_currency = primary_currency;
    }

    public String getSecondary_currency() {
        return secondary_currency;
    }

    public void setSecondary_currency(String secondary_currency) {
        this.secondary_currency = secondary_currency;
    }

    public String getNominal() {
        return nominal;
    }

    public void setNominal(String nominal) {
        this.nominal = nominal;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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
        return country + "," + primary_currency + "," + value + "," + secondary_currency + "," + nominal + "," + diameter + "," + name;
    }
}
