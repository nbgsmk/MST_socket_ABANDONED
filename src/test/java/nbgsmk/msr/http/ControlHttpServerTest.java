package nbgsmk.msr.http;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.manager.ApplicationManager;
import nbgsmk.msr.worker.Worker;
import nbgsmk.msr.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.ServerSocket;
import java.net.URL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlHttpServerTest {
    @Test void statusAndCommandEndpointsWork() throws Exception {
        ApplicationManager manager = new ApplicationManager(config(), new FakeWorker());
        int port;
        try (ServerSocket probe = new ServerSocket(0)) { port = probe.getLocalPort(); }
        try (ControlHttpServer server = new ControlHttpServer("127.0.0.1", port, manager)) {
            server.start();
            assertEquals(200, request(port, "/", "GET"));
            String dashboard = response(port, "/", "GET");
            assertTrue(dashboard.contains("Binance MQTT Worker"));
            assertTrue(dashboard.contains("/live-updates"));
            assertEquals(405, request(port, "/", "POST"));
            for (String path : List.of("/status", "/start", "/stop", "/restart")) {
                assertEquals(200, request(port, path, "GET"));
                assertEquals(200, request(port, path, "POST"));
            }
            assertEquals(200, request(port, "/live-updates", "GET"));
            assertEquals(200, request(port, "/live-updates?systemLog=false&mqtt=true", "POST"));
            assertEquals(false, manager.liveCandleSettings().systemLog());
            assertEquals(true, manager.liveCandleSettings().mqtt());
            assertEquals(400, request(port, "/live-updates?systemLog=maybe", "GET"));
            assertEquals(400, request(port, "/live-updates?unknown=true", "GET"));
            assertEquals(405, request(port, "/status", "PUT"));
            assertEquals(405, request(port, "/start", "DELETE"));
        }
    }

    private static int request(int port, String path, String method) throws Exception {
        HttpURLConnection connection = connection(port, path, method);
        return connection.getResponseCode();
    }
    private static String response(int port, String path, String method) throws Exception {
        HttpURLConnection connection = connection(port, path, method);
        return new String(connection.getInputStream().readAllBytes());
    }
    private static HttpURLConnection connection(int port, String path, String method) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URI("http", null, "127.0.0.1", port,
                path.contains("?") ? path.substring(0, path.indexOf('?')) : path,
                path.contains("?") ? path.substring(path.indexOf('?') + 1) : null, null).toURL().openConnection();
        connection.setRequestMethod(method);
        return connection;
    }
    private static AppConfig config() { return new AppConfig(new AppConfig.BinanceConfig("ws://x", List.of("x")), new AppConfig.MqttConfig("tcp://x", "id", "live", "candles", "logging"), new AppConfig.HttpConfig("localhost", 8080), "stop", 1); }
    private static class FakeWorker implements Worker { public void start() {} public void stop() {} public WorkerStatus status() { return WorkerStatus.STOPPED; } public Throwable lastError() { return null; } }
}
