# Shelly Control Architecture

HydroLeaf Shelly Control keeps all device awareness in the backend. The frontend only deals with logical socket IDs and never sees IP addresses.

## Topology model
- **Room** → **Rack** → **SocketDevice** hierarchy stored in-memory by `ShellyRegistry`.
- Initial data:
  - Room: `MAIN_ROOM` ("Main Room")
  - Racks: `RACK_01`, `RACK_02` in `MAIN_ROOM`
  - Sockets (rack `RACK_01`): `PS01L02` → `192.168.8.45`, `PS01L03` → `192.168.8.46`, `PS01L04` → `192.168.8.47`, `PS01L05` → `192.168.8.48` (prepared).
- Only socket IDs and human names are exposed via REST; IP addresses stay server-side.

## REST API (backend-owned)
All endpoints are rooted at `/api/shelly`.

### Discovery
- `GET /api/shelly/rooms` → list rooms.
- `GET /api/shelly/rooms/{roomId}/racks` → racks within a room.
- `GET /api/shelly/racks/{rackId}/sockets` → sockets within a rack (id, name, rackId).

### Socket control
- `GET /api/shelly/sockets/{socketId}/status` → live status `{socketId, output, powerW?, voltageV?, online, lastUpdated}`.
- `POST /api/shelly/sockets/{socketId}/on` → turn on, returns updated status.
- `POST /api/shelly/sockets/{socketId}/off` → turn off, returns updated status.
- `POST /api/shelly/sockets/{socketId}/toggle` → toggle state, returns updated status.
- `GET /api/shelly/status` → map of all socket statuses.

### Automations (in-memory)
- `POST /api/shelly/automation` → create automation. Types:
  - `TIME_RANGE`: `onTime`, `offTime`, optional `daysOfWeek` (defaults to all days).
  - `INTERVAL_TOGGLE`: `intervalMinutes` (>0), `mode` of `TOGGLE` or `PULSE` (pulse uses `pulseSeconds`, default 5s) and runs every interval.
  - `AUTO_OFF`: `durationMinutes` (>0) and optional `startNow` (default true) to turn on then off after the duration.
- `GET /api/shelly/automation` → list active automations.
- `DELETE /api/shelly/automation/{automationId}` → cancel and remove.

Automations use Spring's `TaskScheduler`; they are lost on application restart.

## Adding a new socket
1. Open `src/main/java/se/hydroleaf/shelly/registry/ShellyRegistry.java`.
2. Add a `SocketDevice.builder()` block in `init()` with the new logical `id`, display `name`, `rackId`, `ip`, and optional non-zero `relayIndex`.
3. Restart the backend. The new socket automatically appears in discovery endpoints and can be controlled without exposing its IP to the frontend.

## Operational notes
- Backend uses Shelly Gen3 RPC endpoints via WebClient with timeouts and error handling. Failed calls are reported as `online=false` with HTTP 502 responses.
- Shelly components are disabled under the `test` Spring profile to keep automated test suites isolated from real hardware.
- CORS is restricted to the configured frontend domain (see `app.cors.allowed-origins`).
