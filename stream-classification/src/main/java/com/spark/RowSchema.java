package com.spark;

import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructField;
import org.apache.spark.sql.types.StructType;

public class RowSchema {
    
    public static StructType getRowSchema() {

        StructField[] fields = new StructField[] {
            DataTypes.createStructField("Severity", DataTypes.DoubleType, true),
            DataTypes.createStructField("Distance(mi)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Street", DataTypes.StringType, true),
            DataTypes.createStructField("City", DataTypes.StringType, true),
            DataTypes.createStructField("State", DataTypes.StringType, true),
            DataTypes.createStructField("Country", DataTypes.StringType, true),
            DataTypes.createStructField("Temperature(F)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Wind_Chill(F)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Humidity(%)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Pressure(in)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Visibility(mi)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Wind_Speed(mph)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Precipitation(in)", DataTypes.DoubleType, true),
            DataTypes.createStructField("Weather_Condition", DataTypes.StringType, true)
        };
        
        return DataTypes.createStructType(fields);
    }
}
