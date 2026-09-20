ARG RUNTIME_PLATFORM=linux/amd64
FROM eclipse-temurin:21.0.12_8-jdk@sha256:92a2a4d7a928d057e7bd999c418d66c26a34eb9a0442f3ab67721c3f88110b2d AS build

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

FROM --platform=${RUNTIME_PLATFORM} eclipse-temurin:21.0.12_8-jre@sha256:49e21e16e3c86eb7816a44a67549910ed090fbeb40c29c525d58bf5e02e91b0f AS runtime

WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /workspace/build/libs/fictional-drug-and-disease-ref-backend-kotlin-all.jar ./app.jar

USER 10001:10001
EXPOSE 18080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
