import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TodoServer {

    static List<String> tasks = new ArrayList<>();

    static void send(HttpExchange exchange, String result)
            throws IOException {

        byte[] data = result.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().set(
            "Content-Type", "application/json; charset=UTF-8"
        );

        exchange.getResponseHeaders().set(
            "Access-Control-Allow-Origin", "*"
        );

        exchange.sendResponseHeaders(200, data.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(data);
        }
    }

    static String escape(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r");
    }

    static String getParameter(String query, String key) {
        if (query == null) return "";

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);

            if (parts.length == 2 && parts[0].equals(key)) {
                try {
                    return URLDecoder.decode(
                        parts[1], StandardCharsets.UTF_8
                    );
                } catch (Exception e) {
                    return "";
                }
            }
        }

        return "";
    }

    public static void main(String[] args) throws Exception {

        HttpServer server = HttpServer.create(
            new InetSocketAddress("localhost", 8080), 0
        );

        server.createContext("/", exchange -> {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                send(exchange, "{\"error\":\"Not found\"}");
                return;
            }

            byte[] page = java.nio.file.Files.readAllBytes(
                java.nio.file.Paths.get("index.html")
            );

            exchange.getResponseHeaders().set(
                "Content-Type", "text/html; charset=UTF-8"
            );

            exchange.sendResponseHeaders(200, page.length);

            try (OutputStream os = exchange.getResponseBody()) {
                os.write(page);
            }
        });

        server.createContext("/tasks", exchange -> {
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < tasks.size(); i++) {
                if (i > 0) json.append(",");
                json.append("\"").append(escape(tasks.get(i))).append("\"");
            }
            json.append("]");
            send(exchange, json.toString());
        });

        server.createContext("/add", exchange -> {
            String task = getParameter(
                exchange.getRequestURI().getRawQuery(), "task"
            );

            if ("POST".equals(exchange.getRequestMethod())
                    && !task.trim().isEmpty()) {
                tasks.add(task);
            }

            send(exchange, "{\"message\":\"Task added\"}");
        });

        server.createContext("/delete", exchange -> {
            String task = getParameter(
                exchange.getRequestURI().getRawQuery(), "task"
            );

            if ("POST".equals(exchange.getRequestMethod())) {
                tasks.remove(task);
            }

            send(exchange, "{\"message\":\"Task deleted\"}");
        });

        server.start();

        System.out.println("Server started!");
        System.out.println("Open http://localhost:8080");
    }
}
