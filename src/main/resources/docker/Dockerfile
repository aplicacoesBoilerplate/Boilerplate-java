FROM maven:3.8.4-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline

COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && apt-get install -y tzdata && \
    ln -fs /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata && \
    apt-get clean

ENV TZ=America/Sao_Paulo

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

CMD ["java", "-XX:+UseContainerSupport", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
