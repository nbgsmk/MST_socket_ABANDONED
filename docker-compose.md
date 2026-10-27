# Main service with Docker Compose

This Compose workflow runs only the production `Main` application. It does not
start `Main2` or an MQTT broker; the broker must already be running on the
historical PC.

## Prerequisites

- Docker Engine with the Compose plugin.
- The historical-PC MQTT broker listening on TCP port `1883`.
- The broker must accept connections from the Docker host. If it runs on a
  different machine, replace `host.docker.internal` in
  `docker/config/main/application.yml` with that machine's reachable address.
- The broker firewall and bind address must allow the connection.

## Start

From the project root:

```bash
docker compose -f docker-compose.main.yml up --build
```

The application is exposed at `http://localhost:8080`. The mounted YAML binds
HTTP to `0.0.0.0` and connects MQTT to `tcp://host.docker.internal:1883`.

## Monitor and control

In another terminal:

```bash
docker compose -f docker-compose.main.yml logs -f main
curl http://localhost:8080/status
curl http://localhost:8080/start
curl http://localhost:8080/stop
curl http://localhost:8080/restart
curl 'http://localhost:8080/live-updates?systemLog=true&mqtt=true'
```

The process remains alive when MQTT is unavailable; logs will show reconnect
attempts while the HTTP API remains available.

## Stop and cleanup

```bash
docker compose -f docker-compose.main.yml down
docker compose -f docker-compose.main.yml down --rmi local
```

Stopping Compose sends the container termination signal to the JVM, allowing
the application's shutdown hook to close the HTTP server, worker, and MQTT
publisher.

## Smoke checks

Validate the rendered configuration before starting:

```bash
docker compose -f docker-compose.main.yml config
```

After startup, confirm `GET /status` returns JSON and that the container stays
running with `docker compose -f docker-compose.main.yml ps`. Check the logs for
the configured external broker address and HTTP readiness. Temporarily stopping
the external broker should not stop the application container; restoring the
broker should allow the existing Paho reconnect behavior to recover.