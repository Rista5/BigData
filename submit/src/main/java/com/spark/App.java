// package com.spark;

import java.util.Arrays;
import java.util.List;

import org.apache.spark.sql.*;

public class App 
{   
    public static void main( String[] args )
    {
        System.out.println("Application started");
        if (args.length < 1) 
        {
            throw new IllegalArgumentException("Csv file path on the hdfs must be passed as argument");
        }

        String sparkMasterUrl = System.getenv("SPARK_MASTER_URL");
        String hdfsUrl = System.getenv("HDFS_URL");
        String dataUrl = args[0];
        
        String csvFileUrl = hdfsUrl + dataUrl;
        String csvEventsUrl = hdfsUrl + args[1];

        if (isNullOrEmpty(sparkMasterUrl)) 
        {
            throw new IllegalStateException("SPARK_MASTER_URL environment variable must be set.");
        }

        if (isNullOrEmpty(hdfsUrl)) 
        {
            throw new IllegalStateException("HDFS_URL environment variable must be set");
        }

        SparkSession spark = SparkSession.builder()
            .appName("Acciedents analysis")
            .master(sparkMasterUrl)
            .getOrCreate();
        spark.sparkContext().setLogLevel("WARN");
        Dataset<Row> dataset = spark.read()
            .option("header", "true")
            .csv(csvFileUrl);
        
        Dataset<Row> events = spark.read()
            .option("header", "true")
            .csv(csvEventsUrl);

        events = events.withColumnRenamed("Code", "OldCode");
        events = events.withColumn("Code", events.col("OldCode").multiply(1.0));
        events = events.select("Code", "Description", "N");
        events.show(100);

        System.out.println("Most Frequent Accident In City");
        mostFrequentAccidentInCity(dataset, events, "Los Angeles", 
            "2015-03-09 07:00:00", "2021-03-09 07:00:00", 100);

        System.out.println("Average Duration Per Accident Type In City");
        averageDurationPerAccidentTypeInCity(dataset, events, "Los Angeles", 
            "2015-03-09 07:00:00", "2021-03-09 07:00:00");

        System.out.println("Weather conditions during accidents");
        weatherConditionsDuringAcciedents(dataset, "Los Angeles", 
            "2015-03-09 07:00:00", "2021-03-09 07:00:00");

        spark.stop();
        spark.close();
    }

    static void mostFrequentAccidentInCity(Dataset<Row> ds, Dataset<Row> events, String city,
        String startTime, String endTime, int countToShow)
    {
        Dataset<Row> filtered = ds.filter(ds.col("City").equalTo(city));
        if (!isNullOrEmpty(startTime))
        {
            filtered = filtered
                .filter(filtered.col("Start_Time").$greater$eq(startTime));
        }
        if (!isNullOrEmpty(endTime))
        {
            filtered = filtered
                .filter(filtered.col("End_Time").$less$eq(endTime));
        }

        filtered = filtered.groupBy(filtered.col("TMC"))
            .agg(functions.count(filtered.col("TMC")));

        filtered.sort(filtered.col("count(TMC)").desc())
            .join(events, filtered.col("TMC").equalTo(events.col("Code")), "left_outer")
            .show(countToShow);
    }

    static void averageDurationPerAccidentTypeInCity(Dataset<Row> ds, Dataset<Row> events, 
        String city, String startTime, String endTime)
    {
        String timeFormat = "yyyy-MM-dd HH:mm:ss";

        Dataset<Row> filt = ds.filter(
            ds.col("City").equalTo(city)
            .and(ds.col("TMC").isNotNull())
            .and(ds.col("Start_Time").$greater$eq(startTime))
            .and(ds.col("End_Time").$less$eq(endTime))
        ).withColumn("Duration", 
            functions.unix_timestamp(ds.col("End_Time"), timeFormat)
                .minus(functions.unix_timestamp(ds.col("Start_Time"), timeFormat)));

        filt.join(events, filt.col("TMC").equalTo(events.col("Code")), "left_outer")
            .groupBy(filt.col("TMC"))
            .agg(functions.avg(filt.col("Duration")),
                functions.min(filt.col("Duration")),
                functions.max(filt.col("Duration")))
            .show();
    }

    static void weatherConditionsDuringAcciedents(Dataset<Row> ds, String city,
        String startTime, String endTime)
    {
        Dataset<Row> filt = ds.filter(
            ds.col("Weather_Condition").isNotNull()
            .and(ds.col("City").equalTo(city))
            .and(ds.col("Start_Time").$greater$eq(startTime))
            .and(ds.col("End_Time").$less$eq(endTime))
        );

        filt = filt.groupBy(filt.col("Weather_Condition"))
            .agg(functions.count(filt.col("Weather_Condition")));
        filt.sort(filt.col("count(Weather_Condition)").desc())
            .show();
    }

    static void cityWithMaxAccidentsInPeiod(Dataset<Row> ds, 
        String startTime, String endTime)
    {
        Dataset<Row> filt = ds.filter(ds.col("Start_Time").$greater$eq(startTime)
                .and(ds.col("End_Time").$less$eq(endTime)))
            .groupBy(ds.col("City"))
            .agg(functions.count(ds.col("City")));

            filt.agg(functions.min(filt.col("count(City)")), 
                    functions.max(filt.col("count(City)")))
                .show();
    }
    
    static boolean isNullOrEmpty(String str)
    {
        return str == null || str.isEmpty();
    }
}
