# BigData
Projects for Big Data course

## Setup
All 3 projects were developed using docker and docker compose.  

Big Data Europe (https://hub.docker.com/u/bde2020) containers were used for hadoop and spark instances and also for application deployment container.
For kafka and zookeeper instances wurstmeister (https://hub.docker.com/u/wurstmeister) containers were used and for Cassandra official docker image was used.

## Setup - Docker-compose files
  
- docker-compose.yaml - contains configuration for hadoop nodes/components containers and spark master and consumer containers, run this docker-compose file before any other
- docker-compose-submit.yaml - runs deploy container for spark submit application
- docker-compose-streaming.yaml - runs zookeeper, kafka and cassandra containers, and also containers for spark streaming producer and spark streaming consumer applications
- docker-compose-model-training.yaml - runs container for spark application for training supervised machine learning model
- docker-compose-stream-classification.yaml - runs zookeeper and kafka containers, and also spark stream producer and spark classification consumer applications

## Dataset

Traffic accidents dataset was used which can be found on the following link (https://smoosavi.org/datasets/us_accidents).  

Dataset contains locations of accidents, duration of accidents, attributes describing location where accident has happen, weather information approximately in the time of event.  

Before deploying application (running deploy container with docker compose), run upload-dataset-to-hdfs shell script to deploy dataset from namenode container to HDFS (this shell script executes commands in namenode container to deploy dataset to HDFS).

## Project 1 - Submit app
Project 1 demonstrates the usage of spark batch processing on Traffic Accidents dataset.  
Following calculations have been performed: 
- mostFrequentAccidentInCity - calculates most frequent types of accidents in some time period(window) and displays top 100 of them
- averageDurationPerAccidentTypeInCity - calculates average duration of each type of accidents in a given time period
- weatherConditionsDuringAcciedents - calculates most frequent weather conditions during accidents in some time period
- cityWithMaxAccidentsInPeiod - find a city with most recorded accidents in time period


Accident types were joined (join column TMC) with descriptions downloaded from wikipedia.

## Project 2 - Spark stream processing

Project 2 consists of 2 applications: **stream-producer** and **stream-consumer**.  
**stream-producer** reads one line at a time from dataset (dataset is saved on HDFS) and publishes it to Kafka **accidents** topic.  
**stream-consumer** reads published data from **accidents** topic and finds
- City with most accident in given time window and saves that value in cities_with_most_accidents Cassandra table
- Maximum and minimum of accident duration, average duration of accident and accident count for given city and time window, results are saved in duration_statistic Cassandra table

## Project 3 - Spark ML

Project 3 also consists of 2 applications: batch-model-training and stream-classification  

**batch-model-training** is app for training ML model for predicting severity of accident (classification) depending on accident location, weather condition during accident, distance of accident.   
Two models were tested: *logistic regression model* and *random forest model* - with random forest achieving more accuracy (around 70%). Models are saved on HDFS along with additional objects that preprocess data before feeding it the model

**stream-classification** is application that loads model and preprocessing objects from HDFS and feeds published data from **accidents** Kafka stream to the model and displays the results.