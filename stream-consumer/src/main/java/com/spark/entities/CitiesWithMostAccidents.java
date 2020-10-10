package com.spark.entities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class CitiesWithMostAccidents {

    private Date startDate;
    private Date endDate;
    private String cities;
    private Long accidentCount;

    public Date getStartDate() {
        return startDate;
    }

    public Long getAccidentCount() {
        return accidentCount;
    }

    public void setAccidentCount(Long accidentCount) {
        this.accidentCount = accidentCount;
    }

    public String getCities() {
        return cities;
    }

    public void setCities(String cities) {
        this.cities = cities;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public CitiesWithMostAccidents(Date startDate, Date endDate, String cities, Long accidentCount) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.cities = cities;
        this.accidentCount = accidentCount;
    }
    
    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String start = formatter.format(startDate);
        String end = formatter.format(endDate);
        return String.format("start: %s\nend: %s\ncities: %s\naccidentCount: %o", start, end, cities, accidentCount);
    }
}
