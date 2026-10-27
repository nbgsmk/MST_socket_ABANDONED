package nbgsmk.msr.worker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.mqtt.MqttPublisher;
import nbgsmk.msr.manager.LiveCandleSettings;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.time.Instant;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class BinanceMarketWorker implements Worker {
    private final AppConfig config;
    private final MqttPublisher publisher;
    private final LiveCandleSettings liveCandleSettings;
    private final ObjectMapper mapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ScheduledExecutorService reconnect = Executors.newSingleThreadScheduledExecutor();
    private volatile WorkerStatus status = WorkerStatus.STOPPED;
    private volatile Throwable lastError;
    private volatile WebSocket socket;
    private volatile boolean requested;
    private volatile long generation;
    private volatile ScheduledFuture<?> reconnectTask;

    public BinanceMarketWorker(AppConfig config, MqttPublisher publisher) {
        this(config, publisher, new LiveCandleSettings(config));
    }

    public BinanceMarketWorker(AppConfig config, MqttPublisher publisher, LiveCandleSettings liveCandleSettings) {
        this.config = config; this.publisher = publisher; this.liveCandleSettings = liveCandleSettings;
    }

    public String streamUrl() { return config.binance().baseUrl() + String.join("/", config.binance().streams()); }

    @Override public synchronized void start() {
        if (requested) return;
        requested = true; status = WorkerStatus.STARTING; publishLog("STARTING", null);
        System.out.println("Worker STARTING; streams=" + config.binance().streams());
        long currentGeneration = ++generation;
        executor.execute(() -> connect(currentGeneration));
    }

    private void connect(long currentGeneration) {
        if (!isCurrent(currentGeneration)) return;
        try {
            WebSocket connected = httpClient.newWebSocketBuilder()
                    .buildAsync(URI.create(streamUrl()), new Listener(currentGeneration)).join();
            synchronized (this) {
                if (!isCurrent(currentGeneration)) {
                    connected.sendClose(WebSocket.NORMAL_CLOSURE, "stale connection");
                    return;
                }
                socket = connected;
            }
            status = WorkerStatus.RUNNING; publishLog("RUNNING", null);
            System.out.println("Worker RUNNING; socket connected");
        } catch (Throwable error) { fail(currentGeneration, error); }
    }

    @Override public synchronized void stop() {
        requested = false;
        ++generation;
        if (reconnectTask != null) reconnectTask.cancel(false);
        reconnectTask = null;
        status = WorkerStatus.STOPPING; publishLog("STOPPING", null);
        System.out.println("Worker STOPPING");
        WebSocket current = socket; socket = null;
        if (current != null) current.sendClose(WebSocket.NORMAL_CLOSURE, "stopped");
        status = WorkerStatus.STOPPED; publishLog("STOPPED", null);
        System.out.println("Worker STOPPED");
    }

    @Override public void restart() { stop(); start(); }
    @Override public WorkerStatus status() { return status; }
    @Override public Throwable lastError() { return lastError; }
    @Override public String socketState() { return socket == null ? "DISCONNECTED" : "CONNECTED"; }
    @Override public void close() { stop(); executor.shutdownNow(); reconnect.shutdownNow(); publisher.close(); }

    private synchronized boolean isCurrent(long currentGeneration) {
        return requested && generation == currentGeneration;
    }

    private void fail(long currentGeneration, Throwable error) {
        if (!isCurrent(currentGeneration)) return;
        lastError = error; status = WorkerStatus.ERROR; publishLog("ERROR", error.getMessage());
        System.err.println("Worker ERROR: " + safeMessage(error));
        synchronized (this) {
            if (isCurrent(currentGeneration)) {
            System.out.println("Worker reconnect scheduled in " + config.reconnectDelaySeconds() + " seconds");
                reconnectTask = reconnect.schedule(() -> connect(currentGeneration), config.reconnectDelaySeconds(), TimeUnit.SECONDS);
            }
        }
    }

    private void publishLog(String event, String error) {
        String detail = error == null ? "null" : quote(error);
        publisher.publish(config.mqtt().loggingTopic(), "{\"event\":\"" + event + "\",\"timestamp\":\"" + Instant.now() + "\",\"error\":" + detail + "}");
    }

    private static String quote(String value) { return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""; }

    void handleMessage(String message) {
        try {
            JsonNode root = mapper.readTree(message);
            JsonNode k = root.path("data").path("k");
            if (!"kline".equals(root.path("data").path("e").asText()) || k.isMissingNode()) return;
            boolean closed = k.path("x").asBoolean(false);
            String payload = "{\"event\":\"kline\",\"symbol\":" + quote(k.path("s").asText()) +
                    ",\"interval\":" + quote(k.path("i").asText()) + ",\"eventTime\":" + root.path("data").path("E").asLong() +
                    ",\"openTime\":" + k.path("t").asLong() + ",\"closeTime\":" + k.path("T").asLong() +
                    ",\"open\":" + quote(k.path("o").asText()) + ",\"high\":" + quote(k.path("h").asText()) +
                    ",\"low\":" + quote(k.path("l").asText()) + ",\"close\":" + quote(k.path("c").asText()) +
                    ",\"volume\":" + quote(k.path("v").asText()) + ",\"closed\":" + closed + "}";
            if (closed || liveCandleSettings.mqtt()) {
                publisher.publish(closed ? config.mqtt().completedTopic() : config.mqtt().realtimeTopic(), payload);
            }
            if (closed || liveCandleSettings.systemLog()) {
                System.out.println((closed ? "CLOSED " : "LIVE ") + message);
            }
        } catch (Exception e) {
            lastError = e;
            System.err.println("Kline parsing error: " + safeMessage(e));
            publishLog("ERROR", e.getMessage());
        }
    }

    private static String safeMessage(Throwable error) {
        String message = error == null ? null : error.getMessage();
        return message == null || message.isBlank() ? error.getClass().getSimpleName() : message;
    }

    private final class Listener implements WebSocket.Listener {
        private final long listenerGeneration;

        private Listener(long listenerGeneration) { this.listenerGeneration = listenerGeneration; }

        public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
            if (isCurrent(listenerGeneration) && last) handleMessage(data.toString());
            webSocket.request(1);
            return null;
        }

        public void onError(WebSocket webSocket, Throwable error) { failIfCurrent(webSocket, listenerGeneration, error); }

        public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
            if (isCurrent(listenerGeneration)) {
                synchronized (BinanceMarketWorker.this) {
                    if (socket == webSocket) socket = null;
                }
                fail(listenerGeneration, new IllegalStateException("closed: " + reason));
            }
            return null;
        }
    }

    private void failIfCurrent(WebSocket webSocket, long currentGeneration, Throwable error) {
        if (isCurrent(currentGeneration)) {
            synchronized (this) {
                if (socket == null || socket == webSocket) socket = null;
            }
            fail(currentGeneration, error);
        }
    }
}
