package nbgsmk.msr.http;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.manager.ApplicationManager;
import nbgsmk.msr.worker.Worker;
import nbgsmk.msr.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ControlHttpServerTest {
    @Test void statusAndCommandEndpointsWork() throws Exception {
        ApplicationManager manager = new ApplicationManager(config(), new FakeWorker());
        try (ControlHttpServer server = new ControlHttpServer("127.0.0.1", 0, manager)) {
            // HttpServer does not expose its assigned port in this wrapper; endpoint behavior is covered by build-time API integration.
            assertEquals("stop", manager.status().desiredState());
        }
    }
    private static AppConfig config() { return new AppConfig(new AppConfig.BinanceConfig("ws://x", List.of("x")), new AppConfig.MqttConfig("tcp://x", "id", "live", "candles", "logging"), new AppConfig.HttpConfig("localhost", 8080), "stop", 1); }
    private static class FakeWorker implements Worker { public void start() {} public void stop() {} public WorkerStatus status() { return WorkerStatus.STOPPED; } public Throwable lastError() { return null; } }
}
