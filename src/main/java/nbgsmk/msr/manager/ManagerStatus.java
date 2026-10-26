package nbgsmk.msr.manager;

import nbgsmk.msr.worker.WorkerStatus;

import java.time.Duration;
import java.util.List;

public record ManagerStatus(String desiredState, WorkerStatus workerState, String socketState,
                            Duration uptime, List<String> streams, String lastError) {}
