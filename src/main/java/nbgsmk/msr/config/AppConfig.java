package nbgsmk.msr.config;

import java.util.List;

public record AppConfig(
        BinanceConfig binance,
        MqttConfig mqtt,
        HttpConfig http,
        String initialState,
        int reconnectDelaySeconds,
        SystemLogConfig systemLog
) {
    public AppConfig {
        if (systemLog == null) systemLog = new SystemLogConfig(true);
        if (binance == null || binance.streams() == null || binance.streams().isEmpty()) {
            throw new IllegalArgumentException("binance.streams must not be empty");
        }
        if (binance.baseUrl() == null || binance.baseUrl().isBlank()) {
            throw new IllegalArgumentException("binance.baseUrl must not be blank");
        }
        if (mqtt == null || mqtt.broker() == null || mqtt.broker().isBlank()) {
            throw new IllegalArgumentException("mqtt.broker must not be blank");
        }
        if (mqtt.realtimeTopic().isBlank() || mqtt.completedTopic().isBlank() || mqtt.loggingTopic().isBlank()) {
            throw new IllegalArgumentException("mqtt topics must not be blank");
        }
        if (http == null || http.port() < 1 || http.port() > 65535) {
            throw new IllegalArgumentException("http.port must be between 1 and 65535");
        }
        if (!"start".equalsIgnoreCase(initialState) && !"stop".equalsIgnoreCase(initialState)) {
            throw new IllegalArgumentException("initialState must be start or stop");
        }
        if (reconnectDelaySeconds < 0) {
            throw new IllegalArgumentException("reconnectDelaySeconds must not be negative");
        }
    }

    public AppConfig(BinanceConfig binance, MqttConfig mqtt, HttpConfig http,
                     String initialState, int reconnectDelaySeconds) {
        this(binance, mqtt, http, initialState, reconnectDelaySeconds, new SystemLogConfig(true));
    }

    public record BinanceConfig(String baseUrl, List<String> streams) {}
    public record MqttConfig(String broker, String clientId, String realtimeTopic, String completedTopic,
                             String loggingTopic, Boolean publishRealtimeUpdates) {
        public MqttConfig {
            if (publishRealtimeUpdates == null) publishRealtimeUpdates = true;
        }

        public MqttConfig(String broker, String clientId, String realtimeTopic, String completedTopic, String loggingTopic) {
            this(broker, clientId, realtimeTopic, completedTopic, loggingTopic, true);
        }
    }
    public record HttpConfig(String host, int port) {}
    public record SystemLogConfig(Boolean realtimeUpdates) {
        public SystemLogConfig {
            if (realtimeUpdates == null) realtimeUpdates = true;
        }
    }
}
