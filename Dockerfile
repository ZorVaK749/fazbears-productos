# ── Etapa 1: Build ────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# Copiar el wrapper de Maven y el pom primero (cache de dependencias)
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copiar código fuente y compilar
COPY src ./src
RUN ./mvnw clean package -DskipTests

# ── Etapa 2: Runtime (imagen mínima) ──────────────────────────
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
