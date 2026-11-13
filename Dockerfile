# syntax=docker/dockerfile:1
FROM maven:3.9.16-eclipse-temurin-25-noble AS builder

WORKDIR /home/app

# Resolve dependencies before copying frequently changing application sources.
COPY pom.xml .
RUN --mount=type=cache,id=happycows-maven,target=/root/.m2/repository,sharing=locked \
    mvn -B -ntp -Pproduction dependency:go-offline

COPY frontend/package.json frontend/package-lock.json frontend/
# Keep Node outside target so that the final clean package preserves it.
RUN --mount=type=cache,id=happycows-maven,target=/root/.m2/repository,sharing=locked \
    --mount=type=cache,id=happycows-npm,target=/root/.npm,sharing=locked \
    mvn -B -ntp -Pproduction -Dfrontend.installDirectory=/opt/frontend \
        -Dfrontend.dependency.skip=false \
        frontend:install-node-and-npm@install-node-and-npm frontend:npm@npm-install

COPY . .
RUN --mount=type=cache,id=happycows-maven,target=/root/.m2/repository,sharing=locked \
    mvn -B -ntp -Pproduction -DskipTests -Dcache.use=true \
        -Dfrontend.installDirectory=/opt/frontend clean package

FROM eclipse-temurin:25-jre-alpine

WORKDIR /app
RUN apk add --no-cache bash curl

ENV PRODUCTION=true
COPY --from=builder --chmod=755 /home/app/startup.sh ./startup.sh
COPY --from=builder /home/app/target/happycows-*.jar ./happycows.jar

ENTRYPOINT ["./startup.sh", "happycows.jar"]
