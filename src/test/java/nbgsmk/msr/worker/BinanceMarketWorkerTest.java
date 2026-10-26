package nbgsmk.msr.worker;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.mqtt.MqttPublisher;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class BinanceMarketWorkerTest {
    @Test void routesLiveAndClosedKlines() {
        AppConfig c = new AppConfig(new AppConfig.BinanceConfig("wss://x/", List.of("a", "b")),
                new AppConfig.MqttConfig("tcp://x", "id", "live", "candles", "logging"),
                new AppConfig.HttpConfig("localhost", 8080), "stop", 1);
        FakePublisher p = new FakePublisher(); BinanceMarketWorker worker = new BinanceMarketWorker(c, p);
        worker.handleMessage(message(false)); worker.handleMessage(message(true));
        assertEquals(List.of("live", "candles"), p.topics);
        assertTrue(p.payloads.get(1).contains("\"closed\":true"));
        assertEquals("wss://x/a/b", worker.streamUrl());
    }
    private static String message(boolean closed) { return "{\"data\":{\"e\":\"kline\",\"E\":1,\"k\":{\"t\":1,\"T\":2,\"s\":\"BTCUSDT\",\"i\":\"1m\",\"o\":\"1\",\"h\":\"2\",\"l\":\"0\",\"c\":\"1.5\",\"v\":\"3\",\"x\":" + closed + "}}}"; }
    private static class FakePublisher implements MqttPublisher { List<String> topics = new ArrayList<>(); List<String> payloads = new ArrayList<>(); public void publish(String t, String p) { topics.add(t); payloads.add(p); } }
}
