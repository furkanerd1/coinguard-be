# Stage 1 : build stage - create a JAR file using Maven with JDK
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

# First, we copy the Maven wrapper and pom.xml to download dependencies without copying the entire source code
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline

# copy the source code and build the application
COPY src ./src
RUN ./mvnw clean package -DskipTests

# Stage 2: runtime stage - use a lightweight JRE image to run the application
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# copy the built JAR file from the build stage
COPY --from=build /app/target/*.jar app.jar

# port we want to expose
EXPOSE 8081

# command to run the application
ENTRYPOINT ["java", "-jar", "app.jar"]