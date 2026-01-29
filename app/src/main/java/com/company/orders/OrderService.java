package com.company.orders;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

public class OrderService {

    public static void main(String[] args) throws IOException {

        int port = 8080;
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        server.createContext("/health", new HealthHandler());
        server.createContext("/orders", new OrdersHandler());

        server.setExecutor(null);
        server.start();

        System.out.println("Customer Order Service started on port " + port);
    }

    static class HealthHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response = "{ \"status\": \"UP\" }";
            send(exchange, response);
        }
    }

    static class OrdersHandler implements HttpHandler {
        public void handle(HttpExchange exchange) throws IOException {
            String response =
                "[ { \"orderId\": 101, \"item\": \"Laptop\", \"status\": \"DELIVERED\" }," +
                "  { \"orderId\": 102, \"item\": \"Phone\", \"status\": \"SHIPPED\" } ]";
            send(exchange, response);
        }
    }

    private static void send(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, response.length());
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }
}

