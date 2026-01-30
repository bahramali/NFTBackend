# API reference (English)

This page enumerates every HTTP endpoint exposed by the backend. Unless noted, endpoints expect/return JSON and use the `Authorization` header with a bearer token for protected routes.

## Health
- `GET /api/health` — liveness check, returns `OK`.

## Authentication
- `POST /api/auth/login` — exchange email/password for access token.
- `POST /api/auth/register` — customer self-registration and login.
- `POST /api/auth/accept-invite` — accept an admin invite (token + password) and login.
- `GET /api/auth/accept-invite/{token}` — validate invite token and read metadata.
- `POST /api/auth/password-reset` — request a password reset (uses `Authorization` token if available).
- `GET /api/auth/oauth/providers` — list supported OAuth providers.
- `POST /api/auth/oauth/google/start` — start Google OAuth login and return `authorizationUrl` (use this endpoint; `/api/auth/oauth/google/login` is not implemented).
- `GET /api/auth/oauth/google/callback` — finish Google OAuth login (`code`, `state`).

## Users (admin or TEAM permission)
- `GET /api/users` — list users (requires `TEAM` permission).
- `GET /api/users/{id}` — fetch user by id (requires `TEAM` permission).
- `POST /api/users` — create user (role/permissions changes require `SUPER_ADMIN`).
- `PUT /api/users/{id}` — update user (role/permissions changes require `SUPER_ADMIN`).

## Super admin lifecycle (SUPER_ADMIN only)
- `GET /api/super-admin/admins` — list admin accounts.
- `POST /api/super-admin/admins/invite` — invite an admin.
- `POST /api/super-admin/admins/{id}/resend-invite` — resend invite (optional expiry override).
- `PUT /api/super-admin/admins/{id}/permissions` — update admin permissions.
- `PUT /api/super-admin/admins/{id}/status` — update admin status/active flag.
- `DELETE /api/super-admin/admins/{idOrEmail}` — delete admin by id or email.

## Admin dashboard (ADMIN permissions)
- `GET /api/admin/dashboard` — dashboard summary (`ADMIN_DASHBOARD`).
- `GET /api/admin/orders` — order management summary (`REPORTS`).
- `GET /api/admin/permissions` — show available and granted permissions.

## Admin customers (ADMIN or `CUSTOMERS_VIEW`)
- `GET /api/admin/customers` — list store customers (query params: `sort`, `page`, `size`).

## Worker
- `GET /api/worker/dashboard` — worker dashboard (role `WORKER`).

## Customer
- `GET /api/customer/me` — customer profile (role `CUSTOMER`).
- `GET /api/customer/orders` — customer order summary (role `CUSTOMER`).

## My account (authenticated)
- `GET /api/me` — current profile.
- `PUT /api/me` — update profile.
- `PUT /api/me/profile` — update profile.
- `GET /api/my/devices` — list devices tied to the current user.
- `GET /api/my/devices/{deviceId}` — fetch a specific device.
- `GET /api/store/orders/my` — list store orders for the current user (returns `OrderSummaryDTO[]`).
- `GET /api/store/orders/{orderId}` — fetch a specific store order for the current user (returns `OrderDetailsDTO`).

### Orders API contract (v1)

These DTOs are stable across environments and should be treated as the canonical contract for the Orders page.

#### Endpoints
- `GET /api/store/orders/my` — list store orders for the current authenticated user.
- `GET /api/store/orders/{orderId}` — fetch one store order for the current authenticated user.
- `GET /api/orders/{orderId}` — public order status lookup (no auth).

#### DTO schemas
- `OrderSummaryDTO` (list view)
  ```json
  {
    "orderId": "uuid",
    "orderNumber": "string",
    "createdAt": "instant",
    "lastUpdatedAt": "instant",
    "orderStatus": "OPEN|PROCESSING|SHIPPED|DELIVERED|CANCELLED",
    "paymentStatus": "PENDING|SUCCEEDED|FAILED|REFUNDED",
    "displayStatus": "string",
    "currency": "SEK",
    "totalCents": 0,
    "totalAmountCents": 0,
    "itemsCount": 0,
    "itemsQuantity": 0,
    "paymentProvider": "STRIPE|null",
    "deliveryType": "DELIVERY|PICKUP",
    "paymentAction": {
      "type": "REDIRECT",
      "label": "Continue payment",
      "url": "https://checkout.stripe.com/..."
    }
  }
  ```
- `OrderDetailsDTO` (details view). The `OrderSummaryDTO` fields are **unwrapped**, so detail responses include all summary fields plus:
  ```json
  {
    "email": "string",
    "totals": {
      "subtotalCents": 0,
      "shippingCents": 0,
      "taxCents": 0,
      "discountCents": 0,
      "totalCents": 0
    },
    "shippingAddress": {
      "name": "string",
      "street": "string",
      "zip": "string",
      "city": "string",
      "country": "string"
    },
    "billingAddress": null,
    "payment": {
      "provider": "STRIPE",
      "status": "PENDING|SUCCEEDED|FAILED|REFUNDED",
      "lastError": null,
      "paidAt": "instant|null"
    },
    "items": [
      {
        "productId": "uuid|null",
        "productName": "string",
        "variantId": "uuid|null",
        "variantName": "string|null",
        "quantity": 0,
        "unitPriceCents": 0,
        "lineTotalCents": 0
      }
    ],
    "trackingUrl": null,
    "timeline": []
  }
  ```

#### Status enums + meaning
- `orderStatus`
  - `OPEN` — order created, awaiting payment or processing.
  - `PROCESSING` — payment captured, order is being prepared.
  - `SHIPPED` — handed off to carrier.
  - `DELIVERED` — delivered to customer.
  - `CANCELLED` — order cancelled (no fulfillment).
- `paymentStatus` (mapped from provider)
  - `PENDING` — no payment yet or provider reports "created".
  - `SUCCEEDED` — payment captured.
  - `FAILED` — payment failed or was cancelled.
  - `REFUNDED` — payment refunded.

#### `paymentAction` (when present, and how to use)
- `paymentAction` is present **only** when the order can still be paid:
  - the order is `OPEN`, and
  - the latest payment is not `PAID`, `CANCELLED`, or `REFUNDED`, and
  - the provider is Stripe (or there is no payment record yet).
- When present, the front-end should render a "Continue payment" action and **redirect the user to `paymentAction.url`**. If `paymentAction` is `null`, hide the action.

## Devices & sensors (admin/operator)
- `GET /api/devices` — list devices.
- `GET /api/devices/composite-ids?system&layer&deviceId?` — list composite ids.
- `GET /api/devices/all` — devices with sensors.
- `GET /api/devices/sensors?compositeIds=...` — sensors for selected composite ids.

## Status & history (admin/operator)
- `GET /api/status/{system}/{layer}/{sensorType}/average` — average reading.
- `GET /api/status/{system}/{layer}/all/average` — averages for all sensor types.
- `GET /api/records/history/aggregated` — aggregated history (query params: `compositeId`, `from`, `to`, `bucket`, `sensorType`, `bucketLimit`, `bucketOffset`, `sensorLimit`, `sensorOffset`).
- `POST /api/records/history/aggregated` — same as above via POST.
- `GET /api/topics/sensors` — sensor types grouped by topic.

### Telemetry payloads (MQTT) — AS7343 counts
- The backend expects AS7343 data under `as7343_counts` in telemetry payloads.
- Each entry is stored as a separate sensor type with the prefix `as7343_counts_` and the original key appended.
- Values are stored as numeric `counts` without aggregation or renaming at ingest time.

Example payload fragment:
```json
{
  "timestamp": "2025-01-01T00:00:00Z",
  "as7343_counts": {
    "405nm": 101.0,
    "VIS1": 202.0
  }
}
```

Resulting sensor types stored by the backend:
- `as7343_counts_405nm`
- `as7343_counts_VIS1`

Frontend contract:
- Use the key names exactly as emitted by the device when querying history or live sensor lists.
- The backend does **not** normalize `nm` naming today (e.g., `405nm` vs `nm405`), so consistency must be handled at the device/ingest layer if needed.

## Germination (admin/operator)
- `GET /api/germination` — current germination status.
- `POST /api/germination/start` — trigger start time to now.
- `PUT /api/germination` — update start time.

## Actuators (admin/operator, only when `mqtt.enabled=true`)
- `POST /api/actuators/led/command` — publish LED command.
- `POST /api/actuators/led/schedule` — publish LED schedule.

## Sensor configuration
- `GET /api/sensor-config` — list configs.
- `GET /api/sensor-config/{sensorType}` — fetch config.
- `POST /api/sensor-config` — create config.
- `POST /api/sensor-config/{sensorType}` — create config by path.
- `PUT /api/sensor-config/{sensorType}` — update config.
- `DELETE /api/sensor-config/{sensorType}` — delete config.

## Notes
- `GET /api/notes` — list notes.
- `GET /api/notes/search?query=...` — search notes by content.
- `POST /api/notes` — create note.
- `PUT /api/notes/{id}` — update note.
- `DELETE /api/notes/{id}` — delete note.

## Generic catalog
- `GET /api/products` — demo product list.

## Store (public store API)
- `GET /api/store/products` — list store products (optional `active`).
- `GET /api/store/products/{id}` — fetch store product.
- `POST /api/store/cart` — create or reuse cart by session id.
- `GET /api/store/cart/{cartId}` — get cart with server pricing.
- `POST /api/store/cart/{cartId}/items` — add/merge item.
- `PATCH /api/store/cart/{cartId}/items/{itemId}` — update item quantity.
- `DELETE /api/store/cart/{cartId}/items/{itemId}` — remove item.
- `POST /api/store/checkout` — checkout a cart.
- `POST /api/store/webhook/stripe` — Stripe webhook receiver.
- `POST /api/payments/webhook/nets` — Nets webhook receiver.
- `GET /api/orders/{orderId}` — public order status lookup.
- `POST /api/checkout/sessions` — create a hosted checkout session by order id.
- `/* /api/store/**` — unhandled store routes return 404 (fallback).

### Store admin (admin/operator)
- `GET /api/admin/products` — list products (optional `active`).
- `GET /api/admin/products/{id}` — fetch product by id.
- `POST /api/admin/products` — create product.
- `PUT /api/admin/products/{id}` — update product.
- `DELETE /api/admin/products/{id}` — delete product.

## Shelly (device control)
- `GET /api/shelly/rooms` — list rooms.
- `GET /api/shelly/rooms/{roomId}/racks` — list racks.
- `GET /api/shelly/racks/{rackId}/sockets` — list sockets.
- `GET /api/shelly/sockets/{socketId}/status` — live socket status.
- `POST /api/shelly/sockets/{socketId}/on` — turn on socket.
- `POST /api/shelly/sockets/{socketId}/off` — turn off socket.
- `POST /api/shelly/sockets/{socketId}/toggle` — toggle socket.
- `GET /api/shelly/status` — all socket statuses.
- `POST /api/shelly/automation` — create automation.
- `GET /api/shelly/automation` — list automations.
- `DELETE /api/shelly/automation/{automationId}` — delete automation.

## Debug (enabled only with `debug.routes.enabled=true` or `debug` profile)
- `GET /api/_debug/routes/admin-customers` — list registered admin-customer routes and active profiles.
