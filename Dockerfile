## Dockerfile
#FROM eclipse-temurin:17-jre
#WORKDIR /app
#ARG JAR_FILE=target/*.jar
#COPY ${JAR_FILE} app.jar
#EXPOSE 8080
#ENTRYPOINT ["java", "-jar", "app.jar"]


FROM maven:3.9.11-amazoncorretto-17 AS build
WORKDIR /opt/app
COPY mvnw pom.xml ./
RUN mvn dependency:go-offline
COPY ./src ./src
RUN mvn clean install

FROM amazoncorretto:17.0.16-al2-native-jdk
# Устанавливаем tzdata и настраиваем часовой пояс
ENV TZ=Europe/Moscow
RUN yum install -y tzdata && \
    ln -sf /usr/share/zoneinfo/${TZ} /etc/localtime && \
    echo ${TZ} > /etc/timezone && \
    yum clean all && rm -rf /var/cache/yum
WORKDIR /opt/app
COPY --from=build /opt/app/target/*.jar /opt/app/*.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/opt/app/*.jar"]