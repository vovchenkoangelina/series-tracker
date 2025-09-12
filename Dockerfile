FROM openjdk:24-ea-18-jdk-slim
WORKDIR /app
COPY target/demo-0.0.1-SNAPSHOT.jar /app/series-tracker.jar
ENTRYPOINT ["java","-jar","series-tracker.jar"]
