ARG RUNTIME_PLATFORM=linux/amd64
FROM eclipse-temurin:21.0.10_7-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY config ./config
COPY src ./src

RUN ./gradlew --no-daemon --max-workers=1 \
    -Dorg.gradle.jvmargs="-Xmx384m -XX:MaxMetaspaceSize=256m" \
    -Dkotlin.compiler.execution.strategy=in-process \
    buildFatJar

FROM --platform=${RUNTIME_PLATFORM} eclipse-temurin:21.0.10_7-jre AS runtime

WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /workspace/build/libs/fictional-drug-and-disease-ref-backend-kotlin-all.jar ./app.jar

USER 10001:10001
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
