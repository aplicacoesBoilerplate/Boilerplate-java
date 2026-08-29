FROM maven:3.9.11-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
COPY .mvn ./.mvn
RUN mvn --batch-mode dependency:go-offline

COPY src ./src
RUN mvn --batch-mode clean package -DskipTests

FROM eclipse-temurin:17-jre
WORKDIR /app

RUN apt-get update && apt-get install -y --no-install-recommends curl tzdata && \
    ln -fs /usr/share/zoneinfo/America/Sao_Paulo /etc/localtime && \
    dpkg-reconfigure -f noninteractive tzdata && \
    apt-get clean && \
    rm -rf /var/lib/apt/lists/* && \
    groupadd --system appgroup && \
    useradd --system --gid appgroup --home-dir /app --shell /usr/sbin/nologin appuser

ENV TZ=America/Sao_Paulo

COPY --from=build --chown=appuser:appgroup /app/target/*.jar app.jar

USER appuser

EXPOSE 8080

CMD ["java", "-XX:+UseContainerSupport", "-XX:InitialRAMPercentage=20.0", "-XX:MaxRAMPercentage=65.0", "-Duser.timezone=America/Sao_Paulo", "-jar", "app.jar"]
