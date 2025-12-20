# Public store backend (hydroleaf.se)

This Spring Boot service powers the Hydroleaf public store with server-priced carts, checkout, and Stripe integration. Money values are always integer cents in the configured currency (`app.store.currency`, default `SEK`).

## Base behavior, access, and rate limiting

- All endpoints live under `/api/store` and are CORS-allowed for `https://hydroleaf.se` and `https://www.hydroleaf.se`.
- Requests return structured errors `{ code, message }`. Too many requests return HTTP 429 with `Retry-After` (seconds until the bucket refills).
- Bucket4j rate limiting is enabled outside the `test` profile. Defaults: 120 capacity, refilling 120 tokens every 60s. The client key is resolved in order: `CF-Connecting-IP` → `X-Real-IP` → `X-Forwarded-For` (first value) → `remoteAddr`.

## Data model (JPA entities)

- `product` — `id` (UUID), `sku` (unique), `name`, `description`, `priceCents`, `currency`, `active`, `inventoryQty`, `imageUrl`, `category`, `createdAt`, `updatedAt`.
- `cart` — `id`, `sessionId` (unique), `userId` (nullable), `status` (`OPEN|CHECKED_OUT|ABANDONED`), `createdAt`, `updatedAt`.
- `cart_item` — `id`, `cart_id`, `product_id`, `qty`, `unitPriceCents`, `lineTotalCents`.
- `store_order` — `id`, `orderNumber` (unique `HL-<epochMillis>`), `userId` (nullable), `email`, `status` (`PENDING_PAYMENT|PAID|CANCELLED|FULFILLED`), `subtotalCents`, `shippingCents`, `taxCents`, `totalCents`, `currency`, `shippingAddress`, `createdAt`.
- `order_item` — `id`, `order_id`, `product_id`, `nameSnapshot`, `unitPriceCents`, `qty`, `lineTotalCents`.
- `payment` — `id`, `order_id`, `provider` (`STRIPE`), `status` (`PENDING|PAID`), `providerRef`, `createdAt`.

Inventory is validated on every cart mutation and during checkout with pessimistic locks to prevent negative stock.

## Key request/response shapes

- `ProductResponse`: `{ id, sku, name, description, priceCents, currency, active, inventoryQty, imageUrl, category, createdAt, updatedAt }`.
- `CartResponse`: `{ id, sessionId, userId, status, items: [{ id, productId, sku, name, qty, unitPriceCents, lineTotalCents, imageUrl, currency }], totals: { subtotalCents, shippingCents, taxCents, totalCents, currency }, updatedAt }`.
- `CheckoutRequest`: `{ cartId, email, shippingAddress: { name, line1, line2?, city, state?, postalCode, country, phone? }, userId? }` → `CheckoutResponse`: `{ orderId, paymentUrl }`.

## Cart lifecycle

- `POST /api/store/cart` creates or retrieves an open cart by `sessionId` (auto-generated when omitted) and attaches `userId` if provided later.
- `GET /api/store/cart/{cartId}` recalculates prices and availability before returning the cart.
- `POST /api/store/cart/{cartId}/items` upserts a product, merging quantities for the same product and enforcing `qty ≥ 1` plus live inventory.
- `PATCH /api/store/cart/{cartId}/items/{itemId}` updates quantity with fresh pricing; `DELETE` removes an item.
- Server-side repricing updates `unitPriceCents`/`lineTotalCents`, recomputes `totals`, and stamps `updatedAt`.

## Checkout, orders, and payments

- Checkout requires an `OPEN` cart with at least one item; optional `userId` is copied to the order when the cart lacks one.
- Inventory is revalidated under pessimistic locks. Currency mismatches or inactive products cause `409 Conflict`.
- Orders capture shipping/tax totals, currency, and a snapshot of product names/prices. Inventory is decremented immediately during checkout.
- A `payment` row is created with `providerRef` set to the Stripe session id when available; otherwise it remains the generated order number while using the fallback URL.
- Order status moves to `PAID` only when the webhook marks the linked payment as paid.

## Stripe integration

- Controlled by `app.stripe.enabled` and `app.stripe.api-key`. When enabled, checkout creates a Stripe Checkout Session with line items for each cart item plus optional shipping/tax, returning `paymentUrl` from Stripe.
- Success/cancel URLs are templated: `app.stripe.success-url` and `app.stripe.cancel-url` (default `.../checkout/success|cancel?orderId={orderId}`).
- Webhook parsing enforces the signature when `app.stripe.webhook-secret` is set; otherwise payloads are deserialized without verification for local testing.
- Only `checkout.session.completed` events are processed; unknown or duplicate sessions are safely logged.

## Configuration knobs

- Pricing: `app.store.shipping-flat-cents` (default 0), `app.store.tax-rate-percent` (default 0), `app.store.currency`.
- Payment fallback: `app.store.fallback-payment-url` (default `https://hydroleaf.se/store/pay/{orderId}`) used when Stripe is disabled or misconfigured.
- Rate limit: `app.store.rate-limit.capacity|refill-tokens|refill-seconds`.

## Seed data

On startup, when no products exist the service seeds three products (starter kit, nutrient A, climate sensor) using the configured currency so the public store is immediately usable without UI hard-coding.
