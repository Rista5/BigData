FROM maven:3.6.1-jdk-8-alpine

RUN mkdir -p /usr/src/app
WORKDIR /usr/src/app

COPY pom.xml ./

RUN mvn dependency:resolve
RUN mvn verify


COPY ./ ./
RUN mvn clean 
RUN mvn package

CMD java -cp target/stream-producer-1.0-SNAPSHOT.jar App