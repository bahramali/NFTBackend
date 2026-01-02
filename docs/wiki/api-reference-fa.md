# مرجع API (فارسی)

این صفحه همه‌ی endpointهای بک‌اند را فهرست می‌کند. مگر خلافش ذکر شود، مسیرهای محافظت‌شده از هدر `Authorization` با توکن استفاده می‌کنند و JSON می‌گیرند/برمی‌گردانند.

## سلامت سرویس
- `GET /api/health` — بررسی سلامت، خروجی `OK`.

## احراز هویت
- `POST /api/auth/login` — دریافت توکن با ایمیل/رمز.
- `POST /api/auth/register` — ثبت‌نام مشتری و ورود.
- `POST /api/auth/accept-invite` — پذیرش دعوت‌نامه ادمین (توکن + رمز) و ورود.
- `GET /api/auth/accept-invite/{token}` — اعتبارسنجی توکن دعوت و مشاهده اطلاعات.
- `POST /api/auth/password-reset` — درخواست بازنشانی رمز (در صورت وجود از توکن `Authorization` استفاده می‌کند).
- `GET /api/auth/oauth/providers` — فهرست سرویس‌های OAuth پشتیبانی‌شده.
- `POST /api/auth/oauth/google/start` — شروع ورود با گوگل و دریافت `authorizationUrl`.
- `GET /api/auth/oauth/google/callback` — تکمیل ورود با گوگل (`code`, `state`).

## کاربران (ادمین یا مجوز TEAM)
- `GET /api/users` — فهرست کاربران (نیازمند مجوز `TEAM`).
- `GET /api/users/{id}` — دریافت کاربر با شناسه (نیازمند مجوز `TEAM`).
- `POST /api/users` — ایجاد کاربر (تغییر نقش/مجوز فقط با `SUPER_ADMIN`).
- `PUT /api/users/{id}` — ویرایش کاربر (تغییر نقش/مجوز فقط با `SUPER_ADMIN`).

## چرخهٔ سوپرادمین (فقط SUPER_ADMIN)
- `GET /api/super-admin/admins` — فهرست ادمین‌ها.
- `POST /api/super-admin/admins/invite` — دعوت ادمین.
- `POST /api/super-admin/admins/{id}/resend-invite` — ارسال مجدد دعوت‌نامه (با قابلیت تغییر انقضا).
- `PUT /api/super-admin/admins/{id}/permissions` — تغییر مجوزهای ادمین.
- `PUT /api/super-admin/admins/{id}/status` — تغییر وضعیت/فعال بودن ادمین.
- `DELETE /api/super-admin/admins/{idOrEmail}` — حذف ادمین با شناسه یا ایمیل.

## داشبورد ادمین (مجوزهای ADMIN)
- `GET /api/admin/dashboard` — خلاصه داشبورد (`ADMIN_DASHBOARD`).
- `GET /api/admin/orders` — خلاصه مدیریت سفارش‌ها (`REPORTS`).
- `GET /api/admin/permissions` — مشاهده مجوزهای موجود و اعطا‌شده.

## مشتریان ادمین (ADMIN یا `CUSTOMERS_VIEW`)
- `GET /api/admin/customers` — فهرست مشتریان فروشگاه (پارامترها: `sort`, `page`, `size`).

## کاربر عملیاتی (Worker)
- `GET /api/worker/dashboard` — داشبورد Worker (نقش `WORKER`).

## مشتری
- `GET /api/customer/me` — پروفایل مشتری (نقش `CUSTOMER`).
- `GET /api/customer/orders` — خلاصه سفارش‌های مشتری (نقش `CUSTOMER`).

## حساب من (Authenticated)
- `GET /api/me` — پروفایل فعلی.
- `PUT /api/me` — به‌روزرسانی پروفایل.
- `PUT /api/me/profile` — به‌روزرسانی پروفایل.
- `GET /api/my/devices` — فهرست دستگاه‌های کاربر.
- `GET /api/my/devices/{deviceId}` — جزئیات یک دستگاه.
- `GET /api/store/orders/my` — فهرست سفارش‌های فروشگاه برای کاربر فعلی.
- `GET /api/store/orders/{orderId}` — جزئیات یک سفارش فروشگاه برای کاربر فعلی.

## دستگاه‌ها و سنسورها (ادمین/اپراتور)
- `GET /api/devices` — فهرست دستگاه‌ها.
- `GET /api/devices/composite-ids?system&layer&deviceId?` — فهرست شناسه‌های ترکیبی.
- `GET /api/devices/all` — دستگاه‌ها به‌همراه سنسورها.
- `GET /api/devices/sensors?compositeIds=...` — سنسورهای دستگاه‌های انتخاب‌شده.

## وضعیت و تاریخچه (ادمین/اپراتور)
- `GET /api/status/{system}/{layer}/{sensorType}/average` — میانگین خوانش‌ها.
- `GET /api/status/{system}/{layer}/all/average` — میانگین همه سنسورها.
- `GET /api/records/history/aggregated` — تاریخچه تجمیعی (پارامترها: `compositeId`, `from`, `to`, `bucket`, `sensorType`, `bucketLimit`, `bucketOffset`, `sensorLimit`, `sensorOffset`).
- `POST /api/records/history/aggregated` — همان تاریخچه تجمیعی با POST.
- `GET /api/topics/sensors` — گروه‌بندی سنسورها بر اساس موضوع.

## جوانه‌زنی (ادمین/اپراتور)
- `GET /api/germination` — وضعیت جوانه‌زنی.
- `POST /api/germination/start` — شروع با زمان فعلی.
- `PUT /api/germination` — به‌روزرسانی زمان شروع.

## عملگرها (ادمین/اپراتور، فقط وقتی `mqtt.enabled=true`)
- `POST /api/actuators/led/command` — ارسال فرمان LED.
- `POST /api/actuators/led/schedule` — ارسال زمان‌بندی LED.

## پیکربندی سنسور
- `GET /api/sensor-config` — فهرست پیکربندی‌ها.
- `GET /api/sensor-config/{sensorType}` — دریافت پیکربندی.
- `POST /api/sensor-config` — ایجاد پیکربندی.
- `POST /api/sensor-config/{sensorType}` — ایجاد پیکربندی با پارامتر مسیر.
- `PUT /api/sensor-config/{sensorType}` — ویرایش پیکربندی.
- `DELETE /api/sensor-config/{sensorType}` — حذف پیکربندی.

## یادداشت‌ها
- `GET /api/notes` — فهرست یادداشت‌ها.
- `GET /api/notes/search?query=...` — جست‌وجوی محتوا.
- `POST /api/notes` — ایجاد یادداشت.
- `PUT /api/notes/{id}` — ویرایش یادداشت.
- `DELETE /api/notes/{id}` — حذف یادداشت.

## کاتالوگ عمومی
- `GET /api/products` — فهرست نمونه محصولات.

## فروشگاه (Public Store)
- `GET /api/store/products` — فهرست محصولات فروشگاه (اختیاری `active`).
- `GET /api/store/products/{id}` — دریافت محصول فروشگاه.
- `POST /api/store/cart` — ساخت/بازیابی سبد با session.
- `GET /api/store/cart/{cartId}` — دریافت سبد با قیمت‌گذاری سرور.
- `POST /api/store/cart/{cartId}/items` — افزودن/ترکیب آیتم.
- `PATCH /api/store/cart/{cartId}/items/{itemId}` — به‌روزرسانی تعداد.
- `DELETE /api/store/cart/{cartId}/items/{itemId}` — حذف آیتم.
- `POST /api/store/checkout` — checkout سبد.
- `POST /api/store/webhook/stripe` — وبهوک Stripe.
- `POST /api/payments/webhook/nets` — وبهوک Nets.
- `GET /api/orders/{orderId}` — مشاهده وضعیت عمومی سفارش.
- `POST /api/checkout/sessions` — ساخت نشست پرداخت با شناسه سفارش.
- `/* /api/store/**` — مسیرهای ناشناخته فروشگاه ۴۰۴ می‌شوند.

### ادمین فروشگاه (ادمین/اپراتور)
- `GET /api/admin/products` — فهرست محصولات (اختیاری `active`).
- `GET /api/admin/products/{id}` — دریافت محصول با شناسه.
- `POST /api/admin/products` — ایجاد محصول.
- `PUT /api/admin/products/{id}` — ویرایش محصول.
- `DELETE /api/admin/products/{id}` — حذف محصول.

## Shelly (کنترل دستگاه)
- `GET /api/shelly/rooms` — فهرست اتاق‌ها.
- `GET /api/shelly/rooms/{roomId}/racks` — فهرست رک‌ها.
- `GET /api/shelly/racks/{rackId}/sockets` — فهرست سوکت‌ها.
- `GET /api/shelly/sockets/{socketId}/status` — وضعیت لحظه‌ای سوکت.
- `POST /api/shelly/sockets/{socketId}/on` — روشن‌کردن سوکت.
- `POST /api/shelly/sockets/{socketId}/off` — خاموش‌کردن سوکت.
- `POST /api/shelly/sockets/{socketId}/toggle` — تغییر وضعیت سوکت.
- `GET /api/shelly/status` — وضعیت همه سوکت‌ها.
- `POST /api/shelly/automation` — ایجاد اتوماسیون.
- `GET /api/shelly/automation` — فهرست اتوماسیون‌ها.
- `DELETE /api/shelly/automation/{automationId}` — حذف اتوماسیون.

## دیباگ (فقط با `debug.routes.enabled=true` یا پروفایل `debug`)
- `GET /api/_debug/routes/admin-customers` — فهرست مسیرهای admin-customer و پروفایل‌های فعال.
