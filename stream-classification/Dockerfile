FROM bde2020/spark-java-template:2.4.3-hadoop2.7

ENV SPARK_APPLICATION_MAIN_CLASS com.spark.App
ENV SPARK_APPLICATION_JAR_NAME stream-classificaton-1.0-SNAPSHOT-jar-with-dependencies

ADD start.sh /

RUN chmod +x /start.sh

CMD ["/bin/bash", "/start.sh"]