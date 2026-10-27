package nbgsmk.msr.example;

import nbgsmk.msr.worker.Worker;
import nbgsmk.msr.worker.WorkerStatus;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class DummyWorker implements Worker {
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
    private volatile WorkerStatus status = WorkerStatus.STOPPED;
    private volatile Throwable lastError;
    private volatile ScheduledFuture<?> work;

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
            System.out.println("Dummy worker is doing work");
        } catch (RuntimeException e) {
            lastError = e;
            status = WorkerStatus.ERROR;
            System.err.println("Dummy worker error: " + e.getMessage());
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