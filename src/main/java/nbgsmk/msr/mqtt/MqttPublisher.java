package nbgsmk.msr.mqtt;

public interface MqttPublisher extends AutoCloseable {
    void publish(String topic, String payload);
    default boolean isConnected() { return false; }
    default Throwable lastError() { return null; }
    @Override default void close() {}
}
