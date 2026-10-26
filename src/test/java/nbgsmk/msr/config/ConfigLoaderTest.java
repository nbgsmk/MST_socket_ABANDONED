package nbgsmk.msr.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigLoaderTest {
    @Test void loadsDefaults() throws Exception {
        AppConfig config = ConfigLoader.load(null);
        assertEquals(2, config.binance().streams().size());
        assertEquals("start", config.initialState());
    }

    @Test void rejectsInvalidState() {
        assertThrows(IllegalArgumentException.class, () -> new AppConfig(
                new AppConfig.BinanceConfig("ws://x", java.util.List.of("x")),
                new AppConfig.MqttConfig("tcp://x", "id", "live", "candles", "logging"),
                new AppConfig.HttpConfig("localhost", 8080), "pause", 1));
    }
}
