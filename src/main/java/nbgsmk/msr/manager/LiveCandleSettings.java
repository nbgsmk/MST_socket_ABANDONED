package nbgsmk.msr.manager;

import nbgsmk.msr.config.AppConfig;

public final class LiveCandleSettings {
    private volatile boolean systemLog;
    private volatile boolean mqtt;

    public LiveCandleSettings(AppConfig config) {
        systemLog = config.systemLog().realtimeUpdates();
        mqtt = config.mqtt().publishRealtimeUpdates();
    }

    public boolean systemLog() { return systemLog; }
    public boolean mqtt() { return mqtt; }
    public synchronized void update(Boolean systemLog, Boolean mqtt) {
        if (systemLog != null) this.systemLog = systemLog;
        if (mqtt != null) this.mqtt = mqtt;
    }
}