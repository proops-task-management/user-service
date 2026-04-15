# ============================================================
# Stage 1: builder
# Purpose: compile source code and package it into a .jar file
# This stage is large (~600MB) — it is never shipped to prod
# ============================================================

# Use Maven 3.9 with JDK 21 as the base image for building
# AS builder = give this stage a name so stage 2 can reference it
FROM maven:3.9-eclipse-temurin-21 AS builder

# Set the working directory inside the container to /app
# All subsequent commands run from this directory
WORKDIR /app

# Copy only pom.xml first (before copying source code)
# Why: Docker caches each layer — if pom.xml hasn't changed,
# the next RUN (download dependencies) will be skipped on rebuild
COPY pom.xml .

# Download all Maven dependencies into the container
# go-offline = download everything needed so builds work without internet
# This layer is cached as long as pom.xml doesn't change
RUN mvn dependency:go-offline

# Copy the source code into the container
COPY src ./src

# Compile the source code and package it into a .jar file
# -DskipTests = skip running tests here (tests run separately in CI)
# Output: /app/target/user-service-0.0.1-SNAPSHOT.jar
RUN mvn package -DskipTests


# ============================================================
# Stage 2: runtime
# Purpose: run the .jar in a minimal, secure image
# Only this stage is shipped — no Maven, no JDK, no source code
# ============================================================

# Use only the JRE (not full JDK) on Alpine Linux
# Alpine = minimal Linux distro (~5MB) — much smaller than Ubuntu
# JRE = only what's needed to RUN Java, not compile it
FROM eclipse-temurin:21-jre-alpine

# Set the working directory inside the runtime container
WORKDIR /app

# Copy the .jar from stage 1 (builder) into this stage
# --from=builder = reference stage 1 by its name
# /app/target/*.jar = the compiled jar from stage 1
# app.jar = rename it to a fixed name for the CMD below
COPY --from=builder /app/target/*.jar app.jar

# Document that this container listens on port 8081
# Note: EXPOSE does not actually open the port — docker run -p does that
EXPOSE 8081

# Switch to a non-root user for security
# Running as root inside a container is dangerous — if the app is
# compromised, the attacker gets root access to the host
USER nobody

# The command to run when the container starts
# Equivalent to: java -jar app.jar
CMD ["java", "-jar", "app.jar"]
