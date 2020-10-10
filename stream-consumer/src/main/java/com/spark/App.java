package com.spark;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.BoundStatement;
import com.datastax.oss.driver.api.core.cql.PreparedStatement;
import com.datastax.oss.driver.api.core.cql.SimpleStatement;
import com.datastax.oss.driver.api.querybuilder.QueryBuilder;
import com.datastax.oss.driver.api.querybuilder.insert.RegularInsert;
import com.spark.comparators.DurationComparator;
import com.spark.comparators.StartTimeComparator;
import com.spark.entities.*;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;

import scala.Tuple2;

public class App {
    public static final String AccidentsKafkaTopic = "accidents";

    public static final String Keyspace = "traffic_accidents";
    public static final String DurationStatisticTable = "duration_statistic";
    public static final String CitiesWIthMostAccidentsTable = "cities_with_most_accidents";

    public static void main(String[] args) {
        System.out.println("Stream consumer starting");
        String initialSleepTime = System.getenv("INITIAL_SLEEP_TIME_IN_SECONDS");
        if (initialSleepTime != null && !initialSleepTime.equals("")) {
            int sleep = Integer.parseInt(initialSleepTime);
            System.out.println("Sleeping on start " + sleep + "sec");
            try {
                Thread.sleep(sleep * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        String sparkMasterUrl = System.getenv("SPARK_MASTER_URL");
        if (sparkMasterUrl == null || sparkMasterUrl.equals("")) {
            throw new IllegalStateException("SPARK_MASTER_URL environment variable must be set.");
        }
        String kafkaUrl = System.getenv("KAFKA_URL");
        if (kafkaUrl == null || kafkaUrl.equals("")) {
            throw new IllegalStateException("KAFKA_URL environment variable must be set");
        }
        String cassandraUrl = System.getenv("CASSANDRA_URL");
        if (cassandraUrl == null || cassandraUrl.equals("")) {
            throw new IllegalStateException("CASSANDRA_URL environment variable must be set");
        }
        String cassandraPortStr = System.getenv("CASSANDRA_PORT");
        if (cassandraPortStr == null || cassandraPortStr.equals("")) {
            throw new IllegalStateException("CASSANDRA_PORT environment variable must be set");
        }
        Integer cassandraPort = Integer.parseInt(cassandraPortStr);
        String dataReceivingTimeInSec = System.getenv("DATA_RECEIVING_TIME_IN_SECONDS");
        if (dataReceivingTimeInSec == null || dataReceivingTimeInSec.equals("")) {
            throw new IllegalStateException("DATA_RECEIVING_TIME_IN_SECONDS environment variable must be set");
        }
        int dataReceivingTime = Integer.parseInt(dataReceivingTimeInSec);

        System.out.println("Consumer started");
        prepareCassandraKeyspace(cassandraUrl, cassandraPort);

        SparkConf conf = new SparkConf().setAppName("Spark Streaming").setMaster(sparkMasterUrl);
        JavaStreamingContext streamingContext = new JavaStreamingContext(conf, new Duration(dataReceivingTime * 1000));
        streamingContext.checkpoint("./checkpoint");
        System.out.println("SPark started");

        Map<String, Object> kafkaParams = getKafkaParams(kafkaUrl);
        Collection<String> topics = Collections.singletonList(AccidentsKafkaTopic);
        JavaInputDStream<ConsumerRecord<Object, String>> stream = KafkaUtils.createDirectStream(streamingContext,
                LocationStrategies.PreferConsistent(), ConsumerStrategies.Subscribe(topics, kafkaParams));

        String city = "Dayton";

        JavaDStream<String> receivedData = stream.map(ConsumerRecord::value);
        receivedData.print();
        JavaDStream<TrafficAccidentData> accidents = receivedData.map((line) -> {
            TrafficAccidentData ta = null;
            try {
                ta = TrafficAccidentData.createTrafficAccidentFromLine(line);
            } catch (java.text.ParseException e) {
                System.out.println("Error: " + e.getMessage());
            }
            return ta;
        }).filter((ta) -> ta != null);

        // turn off this filter for testing, because specific cities may be rare
        JavaDStream<TrafficAccidentData> accInCity = accidents.filter(acc -> {
            return acc.getCity().equals(city); 
        });

        accidents.foreachRDD((accRdd) -> {
            Long count = accRdd.count();
            if (count <= 0) {
                System.out.println("Empty RDD, skipping");
                return;
            }

            System.out.println("All Accidents");
            Map<String, Long> cityAccidentCount = accRdd
                    .mapToPair((ta) -> new Tuple2<String, Long>(ta.getCity(), 1l))
                    .reduceByKey((a, b) -> a + b)
                    .collectAsMap();

            TrafficAccidentData starting = accRdd.min(new StartTimeComparator());
            TrafficAccidentData ending = accRdd.max(new StartTimeComparator());

            System.out.println("Accidents in each city");
            Iterator<String> iter =cityAccidentCount.keySet().iterator();
            while(iter.hasNext()) {
                String c = iter.next();
                System.out.println("City: "+c+", AccCount: "+cityAccidentCount.get(c));
            }
            Tuple2<String, Long> cityWithMaxAcc = findCitiesWithMostAccidents(cityAccidentCount);
            CitiesWithMostAccidents cwma = new CitiesWithMostAccidents(starting.getStartTime(), ending.getEndTime(),
                    cityWithMaxAcc._1(), cityWithMaxAcc._2());

            
            System.out.println("CITIES_WITH_MOST_ACCIDENTS");
            System.out.println(cwma.toString());
            saveCitiesWithMostAccidents(cwma, cassandraUrl, cassandraPort);
        });

        accInCity.foreachRDD((accRdd) -> {
            Long count = accRdd.count();
            if (count <= 0) {
                System.out.println("Empty RDD, skipping");
                return;
            }

            System.out.println("Accidents in city");
            
            TrafficAccidentData starting = accRdd.min(new StartTimeComparator());
            TrafficAccidentData ending = accRdd.max(new StartTimeComparator());
            TrafficAccidentData minimum = accRdd.min(new DurationComparator());
            TrafficAccidentData maximum = accRdd.max(new DurationComparator());
            System.out.println(String.format("STATISTICS starting: %s,\nending: %s,\nminimum: %s,\nmaximum: %s", starting,ending,minimum,maximum));

            JavaRDD<Long> durations = accRdd.map((a) -> a.getDuration());
            
            Long durationSum = durations.reduce((duration, accumulator) -> {
                return duration + accumulator;
            });
            Long averageDuration = durationSum / count;
            DurationStatistic statistic = new DurationStatistic(starting.getStartTime(), ending.getEndTime(),
                    maximum.getCity(), maximum.getDuration(), minimum.getCity(), minimum.getDuration(), averageDuration,
                    count);
            
            System.out.println("DURATION_STATISTIC");
            System.out.println(statistic.toString());
            saveDurationStatistic(statistic, cassandraUrl, cassandraPort);
        });

        streamingContext.start();

        try {
            streamingContext.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static Tuple2<String, Long> findCitiesWithMostAccidents(Map<String, Long> cityAccidentCount) {
        Collection<Long> accCounts = cityAccidentCount.values();
        final Long maxAccCount = accCounts.isEmpty() ? 0l : Collections.max(accCounts);

        Set<String> keys = cityAccidentCount.keySet();
        keys.removeIf((k) -> cityAccidentCount.get(k) != maxAccCount);
        
        String result = String.join(",", keys);
        return new Tuple2<String, Long>(result, maxAccCount);
    }

    private static Map<String, Object> getKafkaParams(String kafkaUrl) {
        Map<String, Object> kafkaParams = new HashMap<String, Object>();
        kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
        kafkaParams.put(ConsumerConfig.CLIENT_ID_CONFIG, "streaming-consumer");
        kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "streaming-consumer");
        kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_DOC, "earliest");
        kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest");
        kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);

        return kafkaParams;
    }

    private static void saveDurationStatistic(DurationStatistic statistic, String addr, Integer port) {
        CassandraConnector conn = CassandraConnector.getInstance();
        conn.connect(addr, port);

        CqlSession session = conn.getSession();
        RegularInsert insertInto = QueryBuilder
            .insertInto(Keyspace, DurationStatisticTable)
            .value("start_date", QueryBuilder.bindMarker())
            .value("end_date", QueryBuilder.bindMarker())
            .value("city_with_max_duration", QueryBuilder.bindMarker())
            .value("max_duration", QueryBuilder.bindMarker())
            .value("city_with_min_duration", QueryBuilder.bindMarker())
            .value("min_duration", QueryBuilder.bindMarker())
            .value("avg_duration", QueryBuilder.bindMarker())
            .value("num_of_accidents", QueryBuilder.bindMarker());

        SimpleStatement insertStatement = insertInto.build();
        PreparedStatement preapredStatement = session.prepare(insertStatement);

        BoundStatement boundStatement = preapredStatement.bind()
            .setInstant(0, statistic.getStartDate().toInstant())
            .setInstant(1, statistic.getEndDate().toInstant())
            .setString(2, statistic.getCityWithLongestAccident())
            .setLong(3, statistic.getMaxAccidentDuration())
            .setString(4, statistic.getCitiWithShortestAccidents())
            .setLong(5, statistic.getMinAccidentDuration())
            .setLong(6, statistic.getAverage())
            .setLong(7, statistic.getAccidentCount());

        session.execute(boundStatement);
        conn.close();
    }

    public static void saveCitiesWithMostAccidents(CitiesWithMostAccidents cwma, String addr, Integer port) {
        CassandraConnector conn = CassandraConnector.getInstance();
        conn.connect(addr, port);

        CqlSession session = conn.getSession();
        RegularInsert insertInto = QueryBuilder
            .insertInto(Keyspace, CitiesWIthMostAccidentsTable)
            .value("start_date", QueryBuilder.bindMarker())
            .value("end_date", QueryBuilder.bindMarker())
            .value("city_with_max_accidents", QueryBuilder.bindMarker())
            .value("num_of_accidents", QueryBuilder.bindMarker());

        SimpleStatement insertStatement = insertInto.build();
        PreparedStatement preapredStatement = session.prepare(insertStatement);

        BoundStatement boundStatement = preapredStatement.bind()
            .setInstant(0, cwma.getStartDate().toInstant())
            .setInstant(1, cwma.getEndDate().toInstant())
            .setString(2, cwma.getCities())
            .setLong(3, cwma.getAccidentCount());
        
        session.execute(boundStatement);
        
        session.close();
        conn.close();
    }

    private static void prepareCassandraKeyspace(String addr, Integer port) {
        CassandraConnector conn = CassandraConnector.getInstance();
        conn.connect(addr, port);
        CqlSession session = conn.getSession();

        String createKeyspaceCQL = String.format(
                                        "CREATE KEYSPACE IF NOT EXISTS %s WITH replication "
                                        + "= {'class':'SimpleStrategy', 'replication_factor':1};", 
                                        Keyspace);

        String createDurationStatistic = String.format(
                                        "CREATE TABLE IF NOT EXISTS %s.%s ("
                                        + " start_date timestamp PRIMARY KEY,"
                                        + " end_date timestamp,"
                                        + " city_with_max_duration text,"
                                        + " max_duration bigint,"
                                        + " city_with_min_duration text,"
                                        + " min_duration bigint,"
                                        + " avg_duration bigint,"
                                        + " num_of_accidents bigint );", 
                                        Keyspace, DurationStatisticTable);
        

        String citiesAccidents = String.format(
                                        "CREATE TABLE IF NOT EXISTS %s.%s ("
                                        + " start_date timestamp PRIMARY KEY,"
                                        + " end_date timestamp,"
                                        + " city_with_max_accidents text,"
                                        + " num_of_accidents bigint );",
                                        Keyspace, CitiesWIthMostAccidentsTable);

        System.out.println("Preparing Cassandra Keyspace");

        session.execute(createKeyspaceCQL);
        System.out.println("Keyspace created");
        session.execute(createDurationStatistic);
        System.out.println(String.format("Table %s created", DurationStatisticTable));
        session.execute(citiesAccidents);
        System.out.println(String.format("Table %s created", CitiesWIthMostAccidentsTable));

        session.close();
        conn.close();
    }
}