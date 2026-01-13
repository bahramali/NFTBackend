# ویکی فروشگاه عمومی Hydroleaf

این صفحه جزئیات API و رفتارهای بک‌اند فروشگاه را به زبان فارسی ارائه می‌دهد؛ پول به‌صورت سنت (مقدار صحیح) و در ارز پیکربندی‌شده (`app.store.currency`، پیش‌فرض `SEK`) مدیریت می‌شود.

## رفتار پایه، دسترسی و محدودسازی نرخ
- همه‌ی مسیرها زیر `/api/store` قرار دارند و برای دامنه‌های `https://hydroleaf.se` و `https://www.hydroleaf.se` در دسترس هستند.
- خطاها ساختار `{ code, message }` دارند. در محدودیت نرخ، پاسخ `429 Too Many Requests` با هدر `Retry-After` (ثانیه تا ریفیل) ارسال می‌شود.
- محدودسازی Bucket4j (خارج از پروفایل `test`) با ظرفیت پیش‌فرض ۱۲۰ درخواست و ریفیل ۱۲۰ توکن در هر ۶۰ ثانیه فعال است. کلید کلاینت به ترتیب از هدرهای `CF-Connecting-IP`، سپس `X-Real-IP`، سپس اولین مقدار `X-Forwarded-For` و در نهایت `remoteAddr` استخراج می‌شود.

## مدل داده (JPA)
- `product`: شناسه UUID، `sku` یکتا، نام، توضیح، `priceCents`، `currency`، وضعیت فعال، `inventoryQty`، `imageUrl`، دسته‌بندی، `createdAt`، `updatedAt`.
- `cart`: شناسه، `sessionId` یکتا، `userId` اختیاری، وضعیت (`OPEN|CHECKED_OUT|ABANDONED`)، مهر زمان ساخت/به‌روزرسانی.
- `cart_item`: شناسه، `cart_id`، `product_id`، `qty`، `unitPriceCents`، `lineTotalCents`.
- `store_order`: شناسه، `orderNumber` یکتا (الگو `HL-<epochMillis>`)، `userId` اختیاری، ایمیل، وضعیت (`PENDING_PAYMENT|PAID|CANCELLED|FULFILLED`)، `subtotalCents`، `shippingCents`، `taxCents`، `totalCents`، `currency`، `shippingAddress`، `createdAt`.
- `order_item`: شناسه، `order_id`، `product_id`، `nameSnapshot`، `unitPriceCents`، `qty`، `lineTotalCents`.
- `payment`: شناسه، `order_id`، `provider` (فقط `STRIPE`)، وضعیت (`PENDING|PAID`)، `providerRef`، `createdAt`.

موجودی در تمام عملیات سبد و Checkout با قفل بدبینانه کنترل می‌شود تا موجودی منفی رخ ندهد.

## اشکال درخواست/پاسخ
- `ProductResponse`: شامل `id`, `sku`, `name`, `description`, `priceCents`, `currency`, `active`, `inventoryQty`, `imageUrl`, `category`, `createdAt`, `updatedAt`.
- `CartResponse`: شامل شناسه سبد، `sessionId`, `userId`, وضعیت، آرایه آیتم‌ها (`id`, `productId`, `sku`, `name`, `qty`, `unitPriceCents`, `lineTotalCents`, `imageUrl`, `currency`)، بخش `totals` (`subtotalCents`, `shippingCents`, `taxCents`, `totalCents`, `currency`) و `updatedAt`.
- `CheckoutRequest`: `{ cartId, email, shippingAddress: { name, line1, line2?, city, state?, postalCode, country, phone? }, userId? }` و پاسخ `CheckoutResponse`: `{ orderId, paymentUrl }`.

## چرخهٔ سبد خرید
- **POST `/api/store/cart`**: ایجاد یا بازیابی سبد باز بر اساس `sessionId` (در صورت نبود، ساخته می‌شود) و اتصال `userId` در صورت ارسال بعدی.
- **GET `/api/store/cart/{cartId}`**: محاسبه‌ی مجدد قیمت و موجودی قبل از بازگشت سبد.
- **POST `/api/store/cart/{cartId}/items`**: افزودن/ترکیب محصول با `qty ≥ 1` و کنترل موجودی زنده.
- **PATCH `/api/store/cart/{cartId}/items/{itemId}`**: به‌روزرسانی تعداد با قیمت روز؛ **DELETE** حذف آیتم.
- محاسبه‌ی سروری قیمت، `unitPriceCents`/`lineTotalCents` و `totals` را به‌روز و `updatedAt` را مهر می‌کند.

## Checkout، سفارش و پرداخت
- Checkout فقط برای سبد `OPEN` با حداقل یک آیتم مجاز است؛ در صورت ارسال `userId` و نبود آن در سبد، به سفارش منتقل می‌شود.
- موجودی و فعال بودن محصول با قفل بدبینانه بررسی می‌شود؛ ناسازگاری ارز یا محصول غیرفعال خطای `409 Conflict` دارد.
- سفارش شامل جمع‌های حمل‌ونقل/مالیات، ارز و اسنپ‌شات نام/قیمت محصول است؛ موجودی هنگام Checkout کم می‌شود.
- رکورد `payment` با `providerRef` معادل `sessionId` استرایپ (در صورت فعال بودن)، وگرنه همان شماره سفارش در حالت لینک پرداخت جایگزین است.
- وضعیت سفارش فقط با علامت‌گذاری پرداخت به `PAID` تغییر می‌کند (وبهوک).

## ادغام استرایپ
- با `app.stripe.enabled` و `app.stripe.secret-key` کنترل می‌شود (متغیر محیطی `STRIPE_SECRET_KEY` با نام مستعار قدیمی `STRIPE_API_KEY`). در حالت فعال، نشست Checkout استرایپ ساخته و `paymentUrl` استرایپ برگردانده می‌شود.
- URL موفق/لغو از قالب‌های `app.stripe.success-url` و `app.stripe.cancel-url` پر می‌شود (پیشنهادی: `https://<domain>/store/order/%s/success|cancel` که `%s` برابر `orderId` است).
- بررسی امضای وبهوک با `app.stripe.webhook-secret` (`STRIPE_WEBHOOK_SECRET`) الزامی است و نبودن امضا/کلید باعث رد شدن رویداد می‌شود.
- تنها رویداد `checkout.session.completed` پردازش می‌شود و جلسات ناشناخته ثبت می‌شوند.

## تنظیمات کلیدی
- قیمت‌گذاری: `app.store.shipping-flat-cents` (پیش‌فرض ۰)، `app.store.tax-rate-percent` (پیش‌فرض ۰)، `app.store.currency`.
- لینک پرداخت جایگزین: `app.store.fallback-payment-url` (پیش‌فرض `https://hydroleaf.se/store/pay/{orderId}`) زمانی که استرایپ غیرفعال یا خطا دارد.
- محدودسازی نرخ: `app.store.rate-limit.capacity|refill-tokens|refill-seconds`.

## دادهٔ اولیه
اگر محصولی وجود نداشته باشد، در شروع سه محصول (کیت استارتر، Nutrient A، حسگر اقلیم) با ارز پیکربندی‌شده اضافه می‌شوند تا فروشگاه بدون نیاز به UI آماده باشد.
