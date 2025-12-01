# Dockerfile
FROM eclipse-temurin:24-jdk-alpine AS builder

# Definir diretório de trabalho
WORKDIR /app

# Copiar arquivos de configuração do Maven
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .

# Dar permissão de execução ao Maven Wrapper
RUN chmod +x mvnw

# Baixar dependências (cache layer)
RUN ./mvnw dependency:go-offline -B

# Copiar código fonte
COPY src ./src

RUN ./mvnw clean package -DskipTests -B

FROM eclipse-temurin:24-jdk-alpine AS production

RUN apk add --no-cache \
    curl \
    tzdata \
    && rm -rf /var/cache/apk/*

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

WORKDIR /app

RUN mkdir -p /app/logs /app/config /app/data && \
    chown -R appuser:appgroup /app

COPY --from=builder --chown=appuser:appgroup /app/target/saga-ecommerce-*.jar app.jar

COPY --chown=appuser:appgroup scripts/docker-entrypoint.sh /app/
RUN chmod +x /app/docker-entrypoint.sh

ENV JAVA_OPTS="-Xmx512m -Xms256m" \
    SPRING_PROFILES_ACTIVE=docker \
    TZ=America/Sao_Paulo \
    SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

USER appuser

ENTRYPOINT ["/app/docker-entrypoint.sh"]
CMD ["java", "-jar", "app.jar"]