FROM gradle:8.12-jdk-alpine as builder
WORKDIR /app
COPY . .
RUN gradle shadowJar

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/parallel-computing-inverted-index.jar .
ENTRYPOINT ["java", "-jar", "parallel-computing-inverted-index.jar"]