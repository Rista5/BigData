package com.spark.entities;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

public class LocalDateHelper {
    
    public static LocalDate convertToLocalDate(Date date) {
        return date.toInstant()
            .atZone(ZoneId.systemDefault())
            .toLocalDate();
    }
}
