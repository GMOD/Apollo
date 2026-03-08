FROM eclipse-temurin:17-jdk AS builder

WORKDIR /build
COPY . .
RUN ./gradlew bootJar --no-daemon -x test

FROM eclipse-temurin:17-jre

RUN useradd -ms /bin/bash -d /apollo apollo
WORKDIR /apollo

COPY --from=builder /build/build/libs/apollo-*.jar apollo.jar

RUN mkdir -p /data && chown -R apollo:apollo /apollo /data

USER apollo

ENV SPRING_PROFILES_ACTIVE=production
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=60s --retries=3 \
    CMD curl -sf http://localhost:8080/apollo/health/index || exit 1

ENTRYPOINT ["java", "-jar", "apollo.jar"]
