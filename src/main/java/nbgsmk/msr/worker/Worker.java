package nbgsmk.msr.worker;

public interface Worker extends AutoCloseable {
    void start();
    void stop();
    default void restart() { stop(); start(); }
    WorkerStatus status();
    Throwable lastError();
    default String socketState() { return status().name(); }
    @Override default void close() { stop(); }
}
