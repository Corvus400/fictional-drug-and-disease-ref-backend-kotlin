ARG RUNTIME_PLATFORM=linux/amd64
FROM eclipse-temurin:21.0.12_8-jdk@sha256:85f00967bcc624fc19fa9c2cf124ea426a5363898e267141726f31f358c2e14b AS build

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

FROM --platform=${RUNTIME_PLATFORM} eclipse-temurin:21.0.12_8-jre@sha256:7a65df4b22d2de92d4e04056e884f3b9122d70b21e2847fd66084278bd0ce037 AS runtime

WORKDIR /app

RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --home-dir /app --shell /usr/sbin/nologin app

COPY --from=build /workspace/build/libs/fictional-drug-and-disease-ref-backend-kotlin-all.jar ./app.jar

USER 10001:10001
EXPOSE 18080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
