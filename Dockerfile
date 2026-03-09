FROM eclipse-temurin:21 AS dev

WORKDIR /app

COPY .mvn/ .mvn
COPY mvnw pom.xml ./

RUN ./mvnw dependency:go-offline

COPY src ./src

RUN ./mvnw clean package -DskipTests

EXPOSE 9000

CMD ["./mvnw", "spring-boot:run"]

FROM eclipse-temurin:21-jre AS prod

WORKDIR /app

COPY --from=dev /app/target/*.jar app.jar

EXPOSE 9000

ENTRYPOINT ["java", "-jar", "app.jar"]