package com.spark;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class TrafficAccidentData implements Serializable {

    /**
     *
     */
    private static final long serialVersionUID = -8808172610346770608L;
    
    private String id;
    private String source;
    private String tmc;
    private String severity;
    private Date startTime;
    private Date endTime;
    private String startLat;
    private String startLng;
    private String endLat;
    private String endLng;
    private String distanceInMeters;
    private String description;
    private String number;
    private String street;
    private String side;
    private String city;
    private String county;
    private String state;
    private String zipcode;
    private String country;
    private String timezone;
    private Long duration;

    private static final String dateFormatString = "yyyy-MM-dd HH:mm:ss";

    public static TrafficAccidentData createTrafficAccidentFromLine(String line) throws java.text.ParseException {
        String[] data = line.split(",");
        return new TrafficAccidentData(data);
    }

    public TrafficAccidentData(String[] data) throws java.text.ParseException{
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormatString);

        id = data[0];
        source = data[1];
        tmc = data[2];
        severity = data[3];
        startTime = sdf.parse(data[4]);
        endTime = sdf.parse(data[5]);
        startLat = data[6];
        startLng = data[7];
        endLat = data[8];
        endLng = data[9];
        distanceInMeters = data[10];
        description = data[11];
        number = data[12];
        street = data[13];
        side = data[14];
        city = data[15];
        county = data[16];
        state = data[17];
        zipcode = data[18];
        country = data[19];
        timezone = data[20];
        duration = calculateDuration(startTime, endTime);
    }

    @Override
    public String toString() {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s", 
            id, source, tmc, severity, startTime, endTime, startLat, startLng, endLat, 
            endLng, distanceInMeters, description, number, street, side, city, county, 
            state, zipcode, country, timezone
        );
    }

    public static long calculateDuration(Date start, Date end) {
        return Duration
            .between(start.toInstant(), end.toInstant())
            .toMillis();
    }

    public Long getDuration() {
        return duration;
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getTMC() {
        return tmc;
    }

    public String getSeverity() {
        return severity;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public String getStartLat() {
        return startLat;
    }

    public String getStartLng() {
        return startLng;
    }
    
    public String getEndLat() {
        return endLat;
    }

    public String getEndLng() {
        return endLng;
    }

    public String getDistanceInMeters() {
        return distanceInMeters;
    }

    public String getDescription() {
        return description;
    }

    public String getNumber() {
        return number;
    }

    public String getStreet() {
        return street;
    }

    public String getSide() {
        return side;
    }

    public String getCity() {
        return city;
    }

    public String getCounty() {
        return county;
    }

    public String getState() {
        return state;
    }

    public String getZipcode() {
        return zipcode;
    }

    public String getCountry() {
        return country;
    }

    public String getTimezone() {
        return timezone;
    }
}