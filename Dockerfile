FROM eclipse-temurin:25-jdk

WORKDIR /app

COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn

RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package

ENTRYPOINT ["java", "-jar", "target/controle-validade-0.0.1-SNAPSHOT.jar"]




