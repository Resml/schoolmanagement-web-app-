FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY lib ./lib
COPY src ./src
RUN mkdir -p bin && javac -cp "lib/*" -d bin src/WebApp.java

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/bin ./bin
COPY --from=builder /app/lib ./lib
ENV PORT=8080
EXPOSE 8080
CMD ["java", "-cp", "bin:lib/*", "WebApp"]
