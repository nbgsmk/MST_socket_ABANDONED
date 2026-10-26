package nbgsmk.msr;

import nbgsmk.msr.config.AppConfig;
import nbgsmk.msr.config.ConfigLoader;
import nbgsmk.msr.http.ControlHttpServer;
import nbgsmk.msr.manager.ApplicationManager;
import nbgsmk.msr.manager.LiveCandleSettings;
import nbgsmk.msr.mqtt.PahoMqttPublisher;
import nbgsmk.msr.worker.BinanceMarketWorker;

import java.util.concurrent.CountDownLatch;

public final class Main {
    private Main() {}

    public static void main(String[] args) throws Exception {
        String configPath = args.length == 2 && "--config".equals(args[0]) ? args[1] : null;
        System.out.println("Loading configuration" + (configPath == null ? " from classpath" : " from " + configPath));
        AppConfig config = ConfigLoader.load(configPath);
        System.out.println("Configuration loaded; initial state=" + config.initialState());
        PahoMqttPublisher publisher = new PahoMqttPublisher(config.mqtt().broker(), config.mqtt().clientId());
        LiveCandleSettings settings = new LiveCandleSettings(config);
        BinanceMarketWorker worker = new BinanceMarketWorker(config, publisher, settings);
        ApplicationManager manager = new ApplicationManager(config, worker, settings);
        ControlHttpServer http = new ControlHttpServer(config.http().host(), config.http().port(), manager);
        CountDownLatch keepAlive = new CountDownLatch(1);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutdown requested; stopping services");
            http.close();
            manager.close();
            worker.close();
            System.out.println("Shutdown cleanup complete");
        }, "shutdown-cleanup"));
        http.start();
        System.out.println("HTTP server ready on " + config.http().host() + ":" + config.http().port());
        System.out.println("Applying initial desired state: " + config.initialState());
        manager.applyInitialState();
        System.out.println("Application started; entering keep-alive wait");
        keepAlive.await();
    }

}
