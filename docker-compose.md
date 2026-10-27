# Main service with Docker Compose

This Compose workflow runs only the production `Main` application. It does not
start `Main2` or an MQTT broker; the broker must already be running on the
historical PC.

## Prerequisites

- Docker Engine with the Compose plugin.
- The historical-PC MQTT broker listening on TCP port `1883`.
- The broker must accept connections from the Docker host. The broker URI is
  selected with `MQTT_BROKER`; the default is
  `tcp://host.docker.internal:1883`.
- The broker firewall and bind address must allow the connection.

## Start

From the project root:

```bash
docker compose -f docker-compose.yml up --build
```

The application is exposed at `http://localhost:8080`. The mounted YAML binds
HTTP to `0.0.0.0`. Set the broker endpoint before starting, without rebuilding:

```bash
MQTT_BROKER=tcp://192.168.1.50:1883 docker compose -f docker-compose.yml up --build
```

You can also use a DNS name, for example
`MQTT_BROKER=tcp://mqtt.example.local:1883`.

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

For a broker running in Podman, publish its port on a host-reachable interface
(for example `-p 1883:1883`) and ensure the broker listens beyond loopback.
Docker and Podman normally have separate networks; `host.docker.internal` is
not a cross-runtime network bridge. If that route times out, use the host's
fixed LAN IP or DNS name instead:

```bash
MQTT_BROKER=tcp://192.168.1.50:1883 docker compose -f docker-compose.yml up -d
```

Verify the selected route from the application container:

```bash
docker compose -f docker-compose.yml exec main getent hosts host.docker.internal
docker compose -f docker-compose.yml exec main curl -v --connect-timeout 3 http://host.docker.internal:1883
```

The HTTP request is expected to fail at the protocol layer; successful TCP
connection is the relevant result. Replace the hostname in that command with
the configured broker host when using a fixed IP or DNS name. A timeout means
to check Podman port publishing, broker bind address, host firewall rules, and
that the address is reachable from the Docker host. Temporarily stopping the
external broker should not stop the application container; restoring it should
allow Paho reconnect behavior to recover.