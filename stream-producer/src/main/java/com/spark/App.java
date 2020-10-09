//package com.spark;

import java.io.IOException;
import java.net.URI;
import java.util.Properties;

import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.LocalFileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hdfs.DistributedFileSystem;
import org.apache.hadoop.conf.Configuration;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

public class App 
{   
    public static final String AccidentsTopic = "accidents";

    public static void main( String[] args )
    {
        String hdfsUrl = System.getenv("HDFS_URL");
        if (hdfsUrl == null || hdfsUrl.equals("")) {
            throw new IllegalStateException("HDFS_URL environment variable must be set.");
        }
        String csvFilePath = System.getenv("CSV_FILE_PATH");
        if (csvFilePath == null || csvFilePath.equals("")) {
            throw new IllegalStateException("CSV_FILE_PATH environment variable must be set");
        }
        String kafkaUrl = System.getenv("KAFKA_URL");
        if (kafkaUrl == null || kafkaUrl.equals("")) {
            throw new IllegalStateException("KAFKA_URL environment variable must be set");
        }
        String publishIntervalInSec = System.getenv("PUBLISH_INTERVAL_IN_SEC");
        if (publishIntervalInSec == null || publishIntervalInSec.equals("")) {
            throw new IllegalStateException("PUBLISH_INTERVAL_IN_SEC environment variable must be set");
        }
        int publishInterval = Integer.parseInt(publishIntervalInSec);

        System.out.println("Producer started");

        KafkaProducer<String, String> producer = createProducer(kafkaUrl);
            
        FileSystem fs = null;
        FSDataInputStream inputStream = null;

        try {
            fs = openHDFS(hdfsUrl);
            inputStream = createHDFSInputStream(fs, csvFilePath);

            String line = inputStream.readLine();
            line = inputStream.readLine(); // skip csv header

            while (line != null) {
                
                ProducerRecord<String, String> rec = new ProducerRecord<String,String>(AccidentsTopic, line);
                producer.send(rec);

                System.out.println("Published line: " + line);
                Thread.sleep(publishInterval * 1000);

                line = inputStream.readLine();
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        } finally {
            try {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (fs != null) {
                    fs.close();
                }   
            } catch (IOException e) {
                System.out.println("Error: "+e.getMessage());
            }
        }

        producer.close();
        System.out.println("Producer has finsihed");
    }

    private static KafkaProducer<String, String> createProducer(String kafkaUrl) {
        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaUrl);
        props.put(ProducerConfig.CLIENT_ID_CONFIG, "stream-producer");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        
        return new KafkaProducer<>(props);
    }

    private static FileSystem openHDFS(String hdfsUrl) throws IOException {
        Configuration conf = new Configuration();
        conf.set(CommonConfigurationKeys.FS_DEFAULT_NAME_DEFAULT, hdfsUrl);
        conf.set("fs.hdfs.impl", DistributedFileSystem.class.getName());
        conf.set("fs.file.impl", LocalFileSystem.class.getName());
        
        return FileSystem.get(URI.create(hdfsUrl), conf);
    }

    private static FSDataInputStream createHDFSInputStream(FileSystem fs, String filePath)
     throws IOException {
        Path path = new Path(filePath);
        if (!fs.exists(path)) {
            throw new IOException("File not found on HDFS");
        }
        
        return fs.open(path);
    }
}