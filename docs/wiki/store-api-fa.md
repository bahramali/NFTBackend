# راهنمای کوتاه فروشگاه عمومی (Hydroleaf)

این صفحه خلاصه‌ای از API و پیش‌نیازهای بک‌اند فروشگاه را به زبان فارسی ارائه می‌دهد.

## پیش‌نیازها و وابستگی‌ها
- جاوا ۱۷ و Maven.
- وابستگی محدودسازی نرخ: `com.bucket4j:bucket4j-core:8.10.1` (دقت کنید گروه `com.bucket4j` است).
- برای پردازش وبهوک استرایپ، `com.google.code.gson:gson:2.11.0` به‌صورت پیش‌فرض به‌عنوان وابستگی کامپایل اضافه شده است.
- خطای ۴۰۳ از ریپازیتوری Maven Central (مثلاً برای `spring-boot-starter-parent`) به تنظیمات شبکه/آینه مرتبط است؛ در محیط CI باید دسترسی ریپو یا آینه مناسب پیکربندی شود.

## خلاصه API عمومی فروشگاه
- **GET `/api/store/products?active=true`**: فهرست محصولات فعال.
- **GET `/api/store/products/{id}`**: دریافت جزئیات محصول.
- **POST `/api/store/cart`**: ساخت/بازیابی سبد با `sessionId` اختیاری و `userId` اختیاری.
- **GET `/api/store/cart/{cartId}`**: دریافت سبد همراه با محاسبه قیمت سروری.
- **POST `/api/store/cart/{cartId}/items`**: افزودن آیتم `{ productId, qty }` با قیمت و موجودی به‌روز.
- **PATCH `/api/store/cart/{cartId}/items/{itemId}`**: به‌روزرسانی تعداد `{ qty }`.
- **DELETE `/api/store/cart/{cartId}/items/{itemId}`**: حذف آیتم.
- **POST `/api/store/checkout`**: ایجاد سفارش و نشست Checkout استرایپ؛ بدنه شامل `cartId`, `email`, و `shippingAddress` است. پاسخ: `{ orderId, paymentUrl }`.
- **POST `/api/store/webhook/stripe`**: وبهوک برای علامت‌گذاری پرداخت موفق (رویداد `checkout.session.completed`).

## قوانین قیمت‌گذاری و موجودی
- تمام مقادیر پولی در «سنت» ذخیره می‌شود؛ محاسبه سروری است و به جمع کل کلاینت اعتماد نمی‌شود.
- قبل از هر تغییر سبد و در زمان Checkout، موجودی و فعال بودن محصول بررسی می‌شود؛ خطای تعارض در کمبود موجودی برگردانده می‌شود.

## محدودسازی نرخ
- تمام مسیرهای `/api/store/**` تحت محدودسازی نرخ Bucket4j هستند؛ کلید بر اساس `X-Forwarded-For` و در نبود آن `remoteAddr` تعیین می‌شود.
