ARG RUNTIME_PLATFORM=linux/amd64
FROM eclipse-temurin:21.0.13.1_2-jdk@sha256:cc9c4fb30bc63de16c227ac3c69f237a10537449ed7ab29fae21c6441470f69c AS build

WORKDIR /workspace

COPY gradlew settings.gradle.kts build.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY config ./config
COPY src ./src

RUN ./gradlew --no-daemon --max-workers=1 \
    -Dorg.gradle.jvmargs="-Xmx384m -XX:MaxMetaspaceSize=256m" \
    -Dkotlin.compiler.execution.strategy=in-process \
    clean buildFatJar \
    && test -f /workspace/build/libs/fictional-drug-and-disease-ref-backend-kotlin-all.jar

FROM --platform=${RUNTIME_PLATFORM} eclipse-temurin:21.0.13.1_2-jre@sha256:b57b0a974dc5ba8b24009a97aeb9ffd178e172727736279e5de73605f40c6813 AS runtime

WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /workspace/build/libs/fictional-drug-and-disease-ref-backend-kotlin-all.jar ./app.jar

USER 10001:10001
EXPOSE 18080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
