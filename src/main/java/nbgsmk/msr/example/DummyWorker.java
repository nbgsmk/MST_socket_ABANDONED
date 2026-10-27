package nbgsmk.msr.example;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.mqtt.MqttPublisher;
import nbgsmk.msr.worker.Worker;
import nbgsmk.msr.worker.WorkerStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class DummyWorker implements Worker {
    private final MqttPublisher publisher;
    private final AppConfig.MqttConfig mqttConfig;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private volatile WorkerStatus status = WorkerStatus.STOPPED;
    private volatile Throwable lastError;
    private volatile ScheduledFuture<?> work;

    public DummyWorker(MqttPublisher publisher, AppConfig.MqttConfig mqttConfig) {
        this.publisher = publisher;
        this.mqttConfig = mqttConfig;
    }

    @Override
    public synchronized void start() {
        if (status == WorkerStatus.RUNNING || status == WorkerStatus.STARTING) return;
        status = WorkerStatus.STARTING;
        work = executor.scheduleAtFixedRate(this::doWork, 0, 5, TimeUnit.SECONDS);
        status = WorkerStatus.RUNNING;
        System.out.println("Dummy worker started");
    }

    @Override
    public synchronized void stop() {
        if (status == WorkerStatus.STOPPED || status == WorkerStatus.STOPPING) return;
        status = WorkerStatus.STOPPING;
        if (work != null) work.cancel(false);
        work = null;
        status = WorkerStatus.STOPPED;
        System.out.println("Dummy worker stopped");
    }

    private void doWork() {
        try {
            String payload = "{\"event\":\"dummy\",\"sequence\":" + System.nanoTime() + "}";
            publishSafely(mqttConfig.realtimeTopic(), payload);
            publishSafely(mqttConfig.completedTopic(), payload);
            publishSafely(mqttConfig.loggingTopic(), payload);
            System.out.println("Dummy worker is doing work and published a sample message");
        } catch (RuntimeException e) {
            lastError = e;
            status = WorkerStatus.ERROR;
            System.err.println("Dummy worker error: " + e.getMessage());
        }
    }

    private void publishSafely(String topic, String payload) {
        try {
            publisher.publish(topic, payload);
        } catch (RuntimeException e) {
            lastError = e;
            status = WorkerStatus.ERROR;
            System.err.println("Dummy worker MQTT error: " + e.getMessage());
        }
    }

    @Override public WorkerStatus status() { return status; }
    @Override public Throwable lastError() { return lastError; }

    @Override
    public synchronized void close() {
        stop();
        executor.shutdownNow();
    }
}