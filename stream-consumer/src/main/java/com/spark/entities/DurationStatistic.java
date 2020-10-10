package com.spark.entities;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DurationStatistic {

    private Date startDate;
    private Date endDate;
    private String cityWithLongestAccident;
    private String citiWithShortestAccident;
    private Long minAccidentDuration;
    private Long maxAccidentDuration;
    private Long average;
    private Long accidentCount;

    public Long getMinAccidentDuration() {
        return minAccidentDuration;
    }

    public String getCitiWithShortestAccidents() {
        return citiWithShortestAccident;
    }

    public Date getEndDate() {
        return endDate;
    }

    public Date getStartDate() {
        return startDate;
    }

    public String getCityWithLongestAccident() {
        return cityWithLongestAccident;
    }

    public Long getMaxAccidentDuration() {
        return maxAccidentDuration;
    }

    public Long getAccidentCount() {
		return accidentCount;
    }
    
	public Long getAverage() {
        return average;
    }

    public void setCityWithLongestAccident(String city) {
        this.cityWithLongestAccident = city;
    }

    public void setCitiWithShortestAccidents(String city) {
        this.citiWithShortestAccident = city;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
    
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

	public void setAccidentCount(Long count) {
		this.accidentCount = count;
	}

    public void setAverage(Long average) {
        this.average = average;
    }

    public void setMaxAccidentDuration(Long max) {
        this.maxAccidentDuration = max;
    }

    public void setMinAccidentDuration(Long min) {
        this.minAccidentDuration = min;
    }

    public DurationStatistic(Date startDate, Date endDate, String cityWithLongestAcc, 
                            Long maxAccDuration, String cityWithShortestAcc, Long minAccDuration, 
                            Long averageAccDuration, Long accidentCount) {
		this.startDate = startDate;
		this.endDate = endDate;
        this.cityWithLongestAccident = cityWithLongestAcc;
        this.citiWithShortestAccident = cityWithShortestAcc;
		this.minAccidentDuration = minAccDuration;
		this.maxAccidentDuration = maxAccDuration;
		this.average = averageAccDuration;
		this.accidentCount = accidentCount;
	}

    @Override
    public String toString() {
        SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        String start = formatter.format(startDate);
        String end = formatter.format(endDate);
        return String.format("startDate: %s\nendDate: %s\ncityWithLongestAccident: %s\ncitiWithShortestAccident: %s\n"
        + "minAccidentDuration: %s\nmaxAccidentDuration: %s\naverage: %s\naccidentCount: %s", start, end,
        cityWithLongestAccident, citiWithShortestAccident, minAccidentDuration, maxAccidentDuration, average,
        accidentCount);
    }
    
}
