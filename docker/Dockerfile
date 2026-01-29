FROM eclipse-temurin:17-jdk

WORKDIR /app

# Copy source code keeping package structure
COPY app/src/main/java ./src

# Compile Java with package structure
RUN javac src/com/company/orders/OrderService.java

EXPOSE 8080

# Run with correct classpath and package name
CMD ["java", "-cp", "src", "com.company.orders.OrderService"]

