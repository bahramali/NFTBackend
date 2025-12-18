# Public store backend (hydroleaf.se)

This service now exposes a minimal but production-ready e-commerce backend for the Hydroleaf public store. All money values are handled in integer cents (SEK) and every cart change re-prices items server-side.

## Data model (JPA entities)

- `product` — `id` (UUID), `sku` (unique), `name`, `description`, `priceCents`, `currency` (SEK), `active`, `inventoryQty`, `imageUrl`, `category`, `createdAt`, `updatedAt`.
- `cart` — `id` (UUID), `sessionId` (unique), `userId` (nullable), `status` (`OPEN|CHECKED_OUT|ABANDONED`), `createdAt`, `updatedAt`.
- `cart_item` — `id`, `cart_id`, `product_id`, `qty`, `unitPriceCents`, `lineTotalCents`.
- `store_order` — `id`, `orderNumber` (unique), `userId` (nullable), `email`, `status` (`PENDING_PAYMENT|PAID|CANCELLED|FULFILLED`), `subtotalCents`, `shippingCents`, `taxCents`, `totalCents`, `currency`, `shippingAddress`, `createdAt`.
- `order_item` — `id`, `order_id`, `product_id`, `nameSnapshot`, `unitPriceCents`, `qty`, `lineTotalCents`.
- `payment` — `id`, `order_id`, `provider` (`STRIPE`), `status`, `providerRef`, `createdAt`.

> Inventory is validated on every cart mutation and during checkout with pessimistic locks on products to prevent negative stock.

## Public endpoints

All endpoints live under `/api/store` and are CORS-allowed for `https://hydroleaf.se` and `https://www.hydroleaf.se`. Requests are rate-limited (bucket of 120 requests per 60s per client IP) and return structured errors `{ code, message }`.

- `GET /api/store/products?active=true` — list products (optionally only active ones).
- `GET /api/store/products/{id}` — fetch a single product.
- `POST /api/store/cart` — create/retrieve an open cart. Body (optional): `{ "sessionId": "...", "userId": "..." }`.
- `GET /api/store/cart/{cartId}` — get cart with recalculated totals.
- `POST /api/store/cart/{cartId}/items` — add item `{ productId, qty }` (qty ≥ 1). Items with the same product accumulate quantity.
- `PATCH /api/store/cart/{cartId}/items/{itemId}` — update quantity `{ qty }` using live pricing.
- `DELETE /api/store/cart/{cartId}/items/{itemId}` — remove an item.
- `POST /api/store/checkout` — create an order and Stripe Checkout session. Body: `{ cartId, email, shippingAddress: { name, line1, line2?, city, state?, postalCode, country, phone? }, userId? }`. Response: `{ orderId, paymentUrl }`.
- `POST /api/store/webhook/stripe` — Stripe webhook to mark orders as paid when a `checkout.session.completed` event arrives.

## Pricing & totals

- The server re-fetches product prices on every cart change and at checkout; client totals are ignored.
- Shipping uses a configurable flat amount (`app.store.shipping-flat-cents`, default `0`). Tax is computed from `app.store.tax-rate-percent` (default `0`).
- `CartResponse` and orders include `subtotalCents`, `shippingCents`, `taxCents`, `totalCents`, and `currency`.

## Stripe checkout

- Controlled by `app.stripe.enabled` (plus `app.stripe.api-key`). When enabled, checkout creates a Stripe Checkout Session with order + shipping + tax line items and returns its `paymentUrl`; otherwise a fallback payment URL template (`app.store.fallback-payment-url`) is returned.
- Webhook verification uses `app.stripe.webhook-secret` when provided; otherwise payloads are parsed without signature enforcement (suitable for local testing).
- Only `checkout.session.completed` updates the payment status to `PAID` and flips the order status to `PAID`.

## Seeded products

On startup, if no products exist the service seeds three real products (starter kit, nutrient A, climate sensor) with SEK pricing so the public store is populated without UI hard-coding.
