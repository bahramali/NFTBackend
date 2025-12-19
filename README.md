# NFTBackend

This project is a Spring Boot application that connects to an MQTT broker and stores incoming messages in a PostgreSQL database. It also forwards the data to WebSocket subscribers.

## MQTT Message Format

Sensor data is published as JSON where each entry in the `sensors` array includes a `sensorType` describing the logical sensor. An optional `sensorName` may identify the hardware instance when multiple sensors of the same type exist, but the backend currently ignores this value. Examples:

### Grow sensors

```json
{
  "deviceId": "esp-1",
  "timestamp": "2024-01-01T00:00:00Z",
  "layer": "test",
  "sensors": [
    { "sensorName": "s450", "sensorType": "spectrum_450", "unit": "count", "value": 10 },
    { "sensorName": "s480", "sensorType": "spectrum_480", "unit": "count", "value": 20 }
  ]
}
```

### Water tank

```json
{
  "deviceId": "tank-1",
  "timestamp": "2024-01-01T00:00:00Z",
  "layer": "test",
  "sensors": [
    { "sensorName": "tank1", "sensorType": "level", "unit": "percent", "value": 70 },
    { "sensorName": "tank1", "sensorType": "temperature", "unit": "C", "value": 22 }
  ]
}
```

## Building and running with Docker

1. Build the Docker image:

```bash
docker build -t nft-backend .
```

2. Run the container with the required environment variables:

```bash
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://<db-host>:5432/<db> \
  -e SPRING_DATASOURCE_USERNAME=<db-user> \
  -e SPRING_DATASOURCE_PASSWORD=<db-pass> \
  -e MQTT_BROKER=tcp://<mqtt-host>:1883 \
  # Optional settings for HTTPS/WSS
  -e SSL_ENABLED=true \
  -e SSL_KEY_STORE=/app/keystore.p12 \
  -e SSL_KEY_STORE_PASSWORD=<changeit> \
  -e SSL_KEY_ALIAS=<alias> \
  nft-backend
```

The resulting image can be pushed to Docker Hub and deployed on AWS or any other Docker-compatible environment.

When the SSL settings are provided the application exposes HTTPS and the WebSocket
endpoint can be reached at `wss://<host>:8080/ws`. Mount the keystore file so that
the container can read it, for example:

```bash
docker run -p 8080:8080 \
  -v /local/path/keystore.p12:/app/keystore.p12 \
  -e SSL_ENABLED=true \
  -e SSL_KEY_STORE=/app/keystore.p12 \
  -e SSL_KEY_STORE_PASSWORD=<changeit> \
  nft-backend
```

## SUPER_ADMIN provisioning

The backend seeds the initial `SUPER_ADMIN` account **only** on server startup when no existing `SUPER_ADMIN` is present. The credentials are read from configuration or environment variables:

- `APP_SUPERADMIN_EMAIL` (required)
- `APP_SUPERADMIN_PASSWORD` (required, minimum 12 characters)
- `APP_SUPERADMIN_DISPLAY_NAME` (optional, defaults to `Super Admin`)
- `APP_SUPERADMIN_ACTIVE` (optional, defaults to `true`)

If the required values are missing, the seed is skipped (no public API or UI can create a `SUPER_ADMIN`).

No `ADMIN`, `WORKER`, or `CUSTOMER` accounts are auto-created; they must be provisioned later via secured APIs. Demo seeds remain disabled by default and can be toggled explicitly with `app.demo-seed.enabled=true` for local development only.

## Admin invite emails

- The backend only logs invite tokens by default to keep local/testing environments simple.
- To deliver real emails, set `APP_INVITE_EMAIL_SMTP_ENABLED=true` (or legacy `APP_INVITE_SMTP_ENABLED=true`) and configure Spring Mail settings (e.g. `SPRING_MAIL_HOST`,
  `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`).
- For Loopia SMTP, use the following values (set `SPRING_MAIL_PASSWORD` to your real password in a `.env` file using the placeholder `LOOPIA_SMTP_PASSWORD`):

  ```env
  SPRING_MAIL_HOST=mailcluster.loopia.se
  SPRING_MAIL_PORT=587
  SPRING_MAIL_USERNAME=no-reply@hydroleaf.se
  SPRING_MAIL_PASSWORD=<LOOPIA_SMTP_PASSWORD>
  APP_INVITE_EMAIL_SMTP_ENABLED=true
  ```
- Optional invite email overrides:
  - `APP_INVITE_EMAIL_FROM` (default: `no-reply@hydroleaf.se`)
  - `APP_INVITE_EMAIL_REPLY_TO` (default: empty, falls back to `APP_INVITE_EMAIL_FROM`)
  - `APP_INVITE_EMAIL_SUBJECT` (default: `You have been invited to Hydroleaf`)
  - `APP_INVITE_LINK_TEMPLATE` (use `{token}` placeholder to embed the invite token in the URL)
  - If `APP_INVITE_EMAIL_FROM` is blank, the backend falls back to the Spring mail username so providers that require matching envelope/from addresses (e.g., Loopia) can accept the message.


## REST Endpoints

* `GET /api/records/history/aggregated` - groups values by sensor and lists timestamp/value pairs. Results are automatically downsampled to roughly 300 points based on the requested time range, discarding zero values when possible. An optional `sensorType` parameter filters the data before aggregation, and bucketing uses TimescaleDB's `time_bucket` for efficiency.

## Public store APIs

A minimal e-commerce backend powers the public Hydroleaf store (`/api/store`). Products, carts, checkout, and Stripe webhook handling are documented in [`docs/wiki/store-public-api.md`](docs/wiki/store-public-api.md), and ready-to-use Bruno/Postman requests live in [`postman/nft-backend.bru`](postman/nft-backend.bru).

## Local development

A separate `application-local.yaml` allows running the service with a Postgres instance on your machine. Start the app using the `local` profile:

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw spring-boot:run
```

Start the database and MQTT broker required for local development using Docker Compose:

```bash
docker compose up -d
```

This brings up a Postgres instance and an MQTT broker on the default ports (`5432` and `1883`).

Modify the values in `src/main/resources/application-local.yaml` if your database settings differ.

## TimescaleDB Setup

This application relies on [TimescaleDB](https://www.timescale.com/) for time-series functions. Ensure your PostgreSQL instance has the extension installed:

```sql
CREATE EXTENSION IF NOT EXISTS timescaledb;
```

The supplied `docker-compose.yml` uses the `timescale/timescaledb` image (PostgreSQL 15) so the extension is available out of the box. When deploying to other environments, install TimescaleDB for your PostgreSQL version and enable the extension manually.

TimescaleDB 2.x on PostgreSQL 15 has been tested. If you build your own PostgreSQL, make sure `shared_preload_libraries` includes `timescaledb`.

## Documentation notes

- For writing right-to-left content (e.g., Farsi) in Markdown without breaking alignment, see [`docs/rtl-formatting.md`](docs/rtl-formatting.md).
- For a Farsi (RTL) technical overview of the service, including a UML sequence of the data flow, see [`docs/overview-fa.html`](docs/overview-fa.html).
- For a quick reference to user management behavior and authentication/authorization flows, see [`docs/wiki/user-management.md`](docs/wiki/user-management.md).
- A Bruno collection covering all REST endpoints (with sample payloads) is available at [`postman/nft-backend.bru`](postman/nft-backend.bru).
