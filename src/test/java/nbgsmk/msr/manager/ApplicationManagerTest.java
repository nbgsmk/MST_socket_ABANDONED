package nbgsmk.msr.manager;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.worker.Worker;
import nbgsmk.msr.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationManagerTest {
    @Test void lifecycleIsIdempotent() {
        FakeWorker worker = new FakeWorker();
        ApplicationManager manager = new ApplicationManager(config("stop"), worker);
        manager.stop(); manager.stop(); manager.start(); manager.restart();
        assertEquals(2, worker.starts);
        assertEquals(3, worker.stops);
        assertEquals("start", manager.status().desiredState());
    }

    private static AppConfig config(String state) {
        return new AppConfig(new AppConfig.BinanceConfig("ws://x", java.util.List.of("x")),
                new AppConfig.MqttConfig("tcp://x", "id", "live", "candles", "logging"),
                new AppConfig.HttpConfig("localhost", 8080), state, 1);
    }

    private static class FakeWorker implements Worker {
        int starts, stops;
        WorkerStatus status = WorkerStatus.STOPPED;
        public void start() { starts++; status = WorkerStatus.RUNNING; }
        public void stop() { stops++; status = WorkerStatus.STOPPED; }
        public WorkerStatus status() { return status; }
        public Throwable lastError() { return null; }
    }
}
