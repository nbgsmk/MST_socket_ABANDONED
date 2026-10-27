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
docker compose -f docker-compose.yml up --build
```

The application is exposed at `http://localhost:8080`. The mounted YAML binds
HTTP to `0.0.0.0` and connects MQTT to `tcp://host.docker.internal:1883`.

## Monitor and control

In another terminal:

```bash
docker compose -f docker-compose.yml logs -f main
docker compose -f docker-compose.yml ps
curl http://localhost:8080/status
curl http://localhost:8080/start
curl http://localhost:8080/stop
curl http://localhost:8080/restart
curl 'http://localhost:8080/live-updates?systemLog=true&mqtt=true'
```

The process remains alive when MQTT is unavailable; logs will show reconnect
attempts while the HTTP API remains available.

`/status` includes `mqttConnected` and `mqttError`. The startup log should show
the configured broker address, followed by either an MQTT connection failure
or a successful connection. A broker failure must not delay the `HTTP server
ready` message or prevent `/status` from responding.

## Stop and cleanup

```bash
docker compose -f docker-compose.yml down
docker compose -f docker-compose.yml down --rmi local
```

Stopping Compose sends the container termination signal to the JVM, allowing
the application's shutdown hook to close the HTTP server, worker, and MQTT
publisher.

## Smoke checks

Validate the rendered configuration before starting:

```bash
docker compose -f docker-compose.yml config
```

After startup, wait until the service shows `healthy`:

```bash
docker compose -f docker-compose.yml ps
```

Then confirm `GET /status` returns JSON and inspect its MQTT fields:

```bash
curl -s http://localhost:8080/status
docker compose -f docker-compose.yml logs --tail=100 main
```

For a broker in another container on the Docker host, publish that broker's
port to the host (for example `1883:1883`) and keep
`host.docker.internal:1883`. Verify the route from the application container:

```bash
docker compose -f docker-compose.yml exec main getent hosts host.docker.internal
docker compose -f docker-compose.yml exec main curl -v --connect-timeout 3 http://host.docker.internal:1883
```

The HTTP request is expected to fail at the protocol layer; successful TCP
connection is the relevant result. If the broker container is not published
to the host, attach the application to its Docker network and use the broker
container's network name instead. Temporarily stopping the external broker
should not stop the application container; restoring it should allow Paho
reconnect behavior to recover.