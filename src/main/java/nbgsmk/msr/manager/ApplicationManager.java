package nbgsmk.msr.manager;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.worker.Worker;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;

public final class ApplicationManager implements AutoCloseable {
    private final Worker worker;
    private final AppConfig config;
    private final ReentrantLock lifecycle = new ReentrantLock();
    private final Instant startedAt = Instant.now();
    private volatile String desiredState;
    private volatile Throwable lastError;

    public ApplicationManager(AppConfig config, Worker worker) {
        this.config = config;
        this.worker = worker;
        this.desiredState = config.initialState().toLowerCase();
    }

    public void start() {
        lifecycle.lock();
        try { desiredState = "start"; worker.start(); } catch (RuntimeException e) { lastError = e; throw e; } finally { lifecycle.unlock(); }
    }

    public void stop() {
        lifecycle.lock();
        try { desiredState = "stop"; worker.stop(); } catch (RuntimeException e) { lastError = e; throw e; } finally { lifecycle.unlock(); }
    }

    public void restart() { lifecycle.lock(); try { desiredState = "start"; worker.restart(); } finally { lifecycle.unlock(); } }

    public ManagerStatus status() {
        Throwable error = worker.lastError() != null ? worker.lastError() : lastError;
        return new ManagerStatus(desiredState, worker.status(), worker.socketState(), uptime(),
                config.binance().streams(), error == null ? null : error.getMessage());
    }

    public Duration uptime() { return Duration.between(startedAt, Instant.now()); }
    public Throwable lastError() { return lastError != null ? lastError : worker.lastError(); }
    public void applyInitialState() { if ("start".equals(desiredState)) start(); else stop(); }
    @Override public void close() { stop(); }
}
