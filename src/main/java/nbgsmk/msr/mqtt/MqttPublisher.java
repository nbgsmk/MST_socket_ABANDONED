package nbgsmk.msr.mqtt;

public interface MqttPublisher extends AutoCloseable {
    void publish(String topic, String payload);
    @Override default void close() {}
}
