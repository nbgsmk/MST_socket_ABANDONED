package nbgsmk.msr.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import nbgsmk.msr.manager.ApplicationManager;
import nbgsmk.msr.manager.ManagerStatus;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;

public final class ControlHttpServer implements AutoCloseable {
    private final ApplicationManager manager;
    private final HttpServer server;

    public ControlHttpServer(String host, int port, ApplicationManager manager) throws IOException {
        this.manager = manager;
        server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/status", this::status);
        server.createContext("/start", exchange -> command(exchange, "start"));
        server.createContext("/stop", exchange -> command(exchange, "stop"));
        server.createContext("/restart", exchange -> command(exchange, "restart"));
        server.setExecutor(Executors.newCachedThreadPool());
    }

    public void start() { server.start(); }

    private void status(HttpExchange exchange) throws IOException {
        if (!"GET".equals(exchange.getRequestMethod())) { send(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        ManagerStatus s = manager.status();
        String error = s.lastError() == null ? "null" : quote(s.lastError());
        String json = "{\"desiredState\":" + quote(s.desiredState()) + ",\"workerState\":" + quote(s.workerState().name()) +
                ",\"socketState\":" + quote(s.socketState()) + ",\"uptimeSeconds\":" + s.uptime().toSeconds() +
                ",\"streams\":[" + s.streams().stream().map(ControlHttpServer::quote).reduce((a,b) -> a + "," + b).orElse("") +
                "],\"lastError\":" + error + "}";
        send(exchange, 200, json);
    }

    private void command(HttpExchange exchange, String command) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) { send(exchange, 405, "{\"error\":\"method not allowed\"}"); return; }
        try {
            switch (command) { case "start" -> manager.start(); case "stop" -> manager.stop(); case "restart" -> manager.restart(); default -> throw new IllegalArgumentException(command); }
            send(exchange, 200, "{\"ok\":true,\"command\":" + quote(command) + "}");
        } catch (IllegalArgumentException e) { send(exchange, 400, "{\"error\":" + quote(e.getMessage()) + "}"); }
        catch (RuntimeException e) { send(exchange, 500, "{\"error\":" + quote(e.getMessage()) + "}"); }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var output = exchange.getResponseBody()) { output.write(bytes); }
    }

    private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }
    @Override public void close() { server.stop(0); }
}
