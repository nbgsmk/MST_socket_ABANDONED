package nbgsmk.msr.example;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.mqtt.MqttPublisher;
import nbgsmk.msr.worker.WorkerStatus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class DummyWorkerTest {
    @Test
    void publishesToAllConfiguredTopics() throws Exception {
        FakePublisher publisher = new FakePublisher();
        DummyWorker worker = new DummyWorker(publisher, mqttConfig());

        worker.start();
        waitForPublications(publisher, 3);
        worker.stop();

        assertEquals(List.of("realtime", "completed", "logging"), publisher.topics.subList(0, 3));
        assertTrue(publisher.payloads.get(0).contains("\"event\":\"dummy\""));
        assertEquals(WorkerStatus.STOPPED, worker.status());
        worker.close();
    }

    @Test
    void publisherFailureDoesNotTerminateScheduledWorker() throws Exception {
        DummyWorker worker = new DummyWorker(new FailingPublisher(), mqttConfig());

        worker.start();
        assertTrue(waitForError(worker));
        assertNotNull(worker.lastError());
        worker.stop();
        worker.close();
        assertEquals(WorkerStatus.STOPPED, worker.status());
    }

    private static AppConfig.MqttConfig mqttConfig() {
        return new AppConfig.MqttConfig("tcp://localhost:1883", "dummy", "realtime", "completed", "logging");
    }

    private static void waitForPublications(FakePublisher publisher, int count) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (publisher.topics.size() < count && System.nanoTime() < deadline) Thread.sleep(10);
        assertTrue(publisher.topics.size() >= count);
    }

    private static boolean waitForError(DummyWorker worker) throws InterruptedException {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
        while (worker.lastError() == null && System.nanoTime() < deadline) Thread.sleep(10);
        return worker.lastError() != null;
    }

    private static final class FakePublisher implements MqttPublisher {
        final List<String> topics = new ArrayList<>();
        final List<String> payloads = new ArrayList<>();

        @Override
        public synchronized void publish(String topic, String payload) {
            topics.add(topic);
            payloads.add(payload);
        }
    }

    private static final class FailingPublisher implements MqttPublisher {
        @Override
        public void publish(String topic, String payload) {
            throw new IllegalStateException("unavailable");
        }
    }
}