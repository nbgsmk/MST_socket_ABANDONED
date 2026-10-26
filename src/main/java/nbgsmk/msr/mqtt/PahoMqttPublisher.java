package nbgsmk.msr.mqtt;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public final class PahoMqttPublisher implements MqttPublisher {
    private final MqttClient client;
    private final MqttConnectOptions options;
    private final ScheduledExecutorService reconnect = Executors.newSingleThreadScheduledExecutor();
    private volatile Throwable lastError;
    private volatile boolean closed;

    public PahoMqttPublisher(String broker, String clientId) throws Exception {
        client = new MqttClient(broker, clientId);
        options = new MqttConnectOptions();
        options.setAutomaticReconnect(true);
        options.setCleanSession(true);
        connect();
    }

    private synchronized void connect() {
        if (closed || client.isConnected()) return;
        try {
            client.connect(options);
            lastError = null;
        } catch (Exception error) {
            lastError = error;
            System.err.println("MQTT connection unavailable: " + safeMessage(error));
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (!closed) reconnect.schedule(this::connect, 5, TimeUnit.SECONDS);
    }

    @Override public void publish(String topic, String payload) {
        if (closed) return;
        if (!client.isConnected()) {
            connect();
            return;
        }
        try {
            client.publish(topic, new MqttMessage(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception error) {
            lastError = error;
            System.err.println("MQTT publish failed: " + safeMessage(error));
            scheduleReconnect();
        }
    }

    @Override public void close() {
        closed = true;
        reconnect.shutdownNow();
        try { if (client.isConnected()) client.disconnect(); client.close(); }
        catch (Exception ignored) { }
    }

    public boolean isConnected() { return client.isConnected(); }
    public Throwable lastError() { return lastError; }

    private static String safeMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }
}
