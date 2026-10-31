ARG RUNTIME_PLATFORM=linux/amd64
FROM eclipse-temurin:21.0.13_9-jdk@sha256:ebfbb18d11ff163ee83b21258acf12248af48d22a0fdfdc68fa1a1516c05976a AS build

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

FROM --platform=${RUNTIME_PLATFORM} eclipse-temurin:21.0.13_9-jre@sha256:0b99b3376fbb50dbf9c0af8535fdd5b8eb0d3fdfc8b1e4967f383b6919f6901a AS runtime

WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /workspace/build/libs/fictional-drug-and-disease-ref-backend-kotlin-all.jar ./app.jar

USER 10001:10001
EXPOSE 18080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
