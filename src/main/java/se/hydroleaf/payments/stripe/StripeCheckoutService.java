package se.hydroleaf.payments.stripe;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.transaction.Transactional;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.common.api.ConflictException;
import se.hydroleaf.common.api.NotFoundException;
import se.hydroleaf.store.config.StripeProperties;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.OrderStatus;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentProvider;
import se.hydroleaf.store.model.PaymentStatus;
import se.hydroleaf.store.model.StoreOrder;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;

@Service
@RequiredArgsConstructor
public class StripeCheckoutService {

    private static final Logger log = LoggerFactory.getLogger(StripeCheckoutService.class);
    private static final String EVENT_SESSION_COMPLETED = "checkout.session.completed";
    private static final String EVENT_SESSION_EXPIRED = "checkout.session.expired";
    private static final String EVENT_PAYMENT_FAILED = "payment_intent.payment_failed";

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final StripeProperties stripeProperties;

    @Transactional
    public StripeCheckoutSessionResponse createCheckoutSession(UUID orderId, String idempotencyKey) throws StripeException {
        StoreOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("ORDER_NOT_FOUND", "Order not found"));
        ensurePayable(order);

        Payment existingPayment = paymentRepository.findByOrderIdAndProvider(orderId, PaymentProvider.STRIPE)
                .orElse(null);
        if (existingPayment != null) {
            if (existingPayment.getStatus() == PaymentStatus.PAID || order.getStatus() == OrderStatus.PAID) {
                throw new ConflictException("ORDER_ALREADY_PAID", "Order is already paid");
            }

            String existingSessionId = existingPayment.getProviderPaymentId();
            if (StringUtils.hasText(existingSessionId) && !"PENDING".equalsIgnoreCase(existingSessionId)) {
                Session existingSession = Session.retrieve(existingSessionId);
                if (isSessionReusable(existingSession)) {
                    updatePaymentIntent(existingPayment, existingSession);
                    return new StripeCheckoutSessionResponse(existingSession.getId(), existingSession.getUrl());
                }
            }
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(formatOrderUrl(stripeProperties.getSuccessUrl(), orderId.toString()))
                .setCancelUrl(formatOrderUrl(stripeProperties.getCancelUrl(), orderId.toString()))
                .addAllLineItem(buildLineItems(order))
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .putMetadata("orderId", orderId.toString())
                        .putMetadata("orderNumber", order.getOrderNumber())
                        .build())
                .setCustomerEmail(order.getEmail())
                .putMetadata("orderId", orderId.toString())
                .putMetadata("orderNumber", order.getOrderNumber())
                .build();

        RequestOptions options = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        Session session = Session.create(params, options);
        Payment payment = existingPayment != null ? existingPayment : Payment.builder()
                .order(order)
                .provider(PaymentProvider.STRIPE)
                .build();
        payment.setStatus(PaymentStatus.CREATED);
        payment.setAmountCents(order.getTotalCents());
        payment.setCurrency(order.getCurrency());
        payment.setProviderPaymentId(session.getId());
        payment.setProviderReference(resolveProviderReference(session));
        paymentRepository.save(payment);

        log.info("Created Stripe checkout session {} for orderId={}", session.getId(), orderId);
        return new StripeCheckoutSessionResponse(session.getId(), session.getUrl());
    }

    @Transactional
    public void handleWebhook(String signature, byte[] payload) {
        if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
            throw new BadRequestException("STRIPE_WEBHOOK_SECRET_MISSING", "Stripe webhook secret is not configured.");
        }

        Event event = parseEvent(signature, payload);
        String eventType = event.getType();
        if (EVENT_SESSION_COMPLETED.equals(eventType)) {
            handleSessionCompleted(extractSession(event));
            return;
        }
        if (EVENT_SESSION_EXPIRED.equals(eventType)) {
            handleSessionExpired(extractSession(event));
            return;
        }
        if (EVENT_PAYMENT_FAILED.equals(eventType)) {
            handlePaymentFailed(extractPaymentIntent(event));
            return;
        }

        log.debug("Ignoring Stripe event type {}", eventType);
    }

    private void ensurePayable(StoreOrder order) {
        if (!stripeProperties.isEnabled() || !StringUtils.hasText(stripeProperties.getSecretKey())) {
            throw new BadRequestException("STRIPE_DISABLED", "Stripe payments are not configured.");
        }
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("ORDER_NOT_PAYABLE", "Order is not pending payment");
        }
        if (order.getTotalCents() <= 0) {
            throw new BadRequestException("ORDER_TOTAL_INVALID", "Order total must be greater than zero");
        }
        if (!"SEK".equalsIgnoreCase(order.getCurrency())) {
            throw new BadRequestException("ORDER_CURRENCY_INVALID", "Order currency must be SEK");
        }
        if (order.getItems() == null || order.getItems().isEmpty()) {
            throw new BadRequestException("ORDER_EMPTY", "Order has no items");
        }
    }

    private List<SessionCreateParams.LineItem> buildLineItems(StoreOrder order) {
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity((long) item.getQty())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency().toLowerCase(Locale.ROOT))
                            .setUnitAmount(item.getUnitPriceCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.getNameSnapshot())
                                    .build())
                            .build())
                    .build());
        }

        if (order.getShippingCents() > 0) {
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency().toLowerCase(Locale.ROOT))
                            .setUnitAmount(order.getShippingCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Shipping")
                                    .build())
                            .build())
                    .build());
        }

        if (order.getTaxCents() > 0) {
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency().toLowerCase(Locale.ROOT))
                            .setUnitAmount(order.getTaxCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Tax")
                                    .build())
                            .build())
                    .build());
        }

        return lineItems;
    }

    private boolean isSessionReusable(Session session) {
        if (session == null) {
            return false;
        }
        String status = session.getStatus();
        if (!StringUtils.hasText(status)) {
            return true;
        }
        return !"expired".equalsIgnoreCase(status);
    }

    private void updatePaymentIntent(Payment payment, Session session) {
        if (payment == null || session == null) {
            return;
        }
        if (payment.getProviderReference() == null && StringUtils.hasText(session.getPaymentIntent())) {
            payment.setProviderReference(session.getPaymentIntent());
            paymentRepository.save(payment);
        }
    }

    private Event parseEvent(String signature, byte[] payload) {
        String payloadString = new String(payload, StandardCharsets.UTF_8);
        try {
            return Webhook.constructEvent(payloadString, signature, stripeProperties.getWebhookSecret());
        } catch (SignatureVerificationException ex) {
            throw new BadRequestException("STRIPE_SIGNATURE_INVALID", "Stripe signature verification failed.");
        }
    }

    private Session extractSession(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject()
                .filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElse(null);
    }

    private PaymentIntent extractPaymentIntent(Event event) {
        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject()
                .filter(PaymentIntent.class::isInstance)
                .map(PaymentIntent.class::cast)
                .orElse(null);
    }

    private StoreOrder resolveOrder(Session session, Payment payment) {
        if (payment != null) {
            return payment.getOrder();
        }
        Map<String, String> metadata = session.getMetadata();
        if (metadata != null && StringUtils.hasText(metadata.get("orderId"))) {
            try {
                UUID orderId = UUID.fromString(metadata.get("orderId"));
                return orderRepository.findById(orderId).orElse(null);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid orderId metadata in Stripe session {}", session.getId());
            }
        }
        return null;
    }

    private StoreOrder resolveOrder(PaymentIntent paymentIntent, Payment payment) {
        if (payment != null) {
            return payment.getOrder();
        }
        Map<String, String> metadata = paymentIntent != null ? paymentIntent.getMetadata() : null;
        if (metadata != null && StringUtils.hasText(metadata.get("orderId"))) {
            try {
                UUID orderId = UUID.fromString(metadata.get("orderId"));
                return orderRepository.findById(orderId).orElse(null);
            } catch (IllegalArgumentException ex) {
                log.warn("Invalid orderId metadata in Stripe payment intent {}", paymentIntent.getId());
            }
        }
        return null;
    }

    private void handleSessionCompleted(Session session) {
        if (session == null) {
            log.warn("Stripe event missing session payload");
            return;
        }

        String sessionId = session.getId();
        if (!StringUtils.hasText(sessionId)) {
            log.warn("Stripe session missing id");
            return;
        }

        Payment payment = paymentRepository.findByProviderPaymentId(sessionId).orElse(null);
        StoreOrder order = resolveOrder(session, payment);
        if (order == null) {
            log.warn("Stripe webhook received for unknown order sessionId={}", sessionId);
            return;
        }

        if (order.getStatus() == OrderStatus.PAID || (payment != null && payment.getStatus() == PaymentStatus.PAID)) {
            log.info("Stripe webhook already processed for sessionId={} orderId={}", sessionId, order.getId());
            return;
        }

        if (!isAmountAndCurrencyValid(session, order)) {
            log.warn("Stripe webhook amount mismatch sessionId={} orderId={}", sessionId, order.getId());
            return;
        }

        if (payment == null) {
            payment = Payment.builder()
                    .order(order)
                    .provider(PaymentProvider.STRIPE)
                    .status(PaymentStatus.PAID)
                    .amountCents(order.getTotalCents())
                    .currency(order.getCurrency())
                    .providerPaymentId(sessionId)
                    .providerReference(resolveProviderReference(session))
                    .build();
        } else {
            payment.setStatus(PaymentStatus.PAID);
            payment.setProviderReference(resolveProviderReference(session));
            payment.setAmountCents(order.getTotalCents());
            payment.setCurrency(order.getCurrency());
        }
        paymentRepository.save(payment);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        log.info("Marked order paid from Stripe webhook orderId={} sessionId={}", order.getId(), sessionId);
    }

    private void handleSessionExpired(Session session) {
        if (session == null) {
            log.warn("Stripe session expired event missing session payload");
            return;
        }

        String sessionId = session.getId();
        if (!StringUtils.hasText(sessionId)) {
            log.warn("Stripe session expired event missing id");
            return;
        }

        Payment payment = paymentRepository.findByProviderPaymentId(sessionId).orElse(null);
        StoreOrder order = resolveOrder(session, payment);
        if (order == null) {
            log.warn("Stripe checkout session expired for unknown order sessionId={}", sessionId);
            return;
        }

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Stripe checkout expired for already paid orderId={} sessionId={}", order.getId(), sessionId);
            return;
        }

        if (payment != null && payment.getStatus() != PaymentStatus.CANCELLED) {
            payment.setStatus(PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
        }

        if (order.getStatus() != OrderStatus.CANCELED) {
            order.setStatus(OrderStatus.CANCELED);
            orderRepository.save(order);
        }

        log.info("Marked order canceled after Stripe checkout expired orderId={} sessionId={}", order.getId(), sessionId);
    }

    private void handlePaymentFailed(PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            log.warn("Stripe payment failed event missing payment intent payload");
            return;
        }

        String paymentIntentId = paymentIntent.getId();
        if (!StringUtils.hasText(paymentIntentId)) {
            log.warn("Stripe payment failed event missing payment intent id");
            return;
        }

        Payment payment = paymentRepository.findByProviderReference(paymentIntentId).orElse(null);
        StoreOrder order = resolveOrder(paymentIntent, payment);
        if (order == null) {
            log.warn("Stripe payment failed for unknown order paymentIntent={}", paymentIntentId);
            return;
        }

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Stripe payment failed for already paid orderId={} paymentIntent={}", order.getId(), paymentIntentId);
            return;
        }

        if (payment != null) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setProviderReference(paymentIntentId);
            paymentRepository.save(payment);
        }

        if (order.getStatus() != OrderStatus.FAILED) {
            order.setStatus(OrderStatus.FAILED);
            orderRepository.save(order);
        }

        log.info("Marked order failed after Stripe payment failure orderId={} paymentIntent={}", order.getId(), paymentIntentId);
    }

    private boolean isAmountAndCurrencyValid(Session session, StoreOrder order) {
        if (session.getAmountTotal() != null && session.getAmountTotal() != order.getTotalCents()) {
            return false;
        }
        if (StringUtils.hasText(session.getCurrency())
                && !session.getCurrency().equalsIgnoreCase(order.getCurrency())) {
            return false;
        }
        return true;
    }

    private String formatOrderUrl(String template, String orderId) {
        if (template == null) {
            return null;
        }
        if (template.contains("%s")) {
            return String.format(template, orderId);
        }
        return template.replace("{orderId}", orderId);
    }

    private String resolveProviderReference(Session session) {
        String paymentIntent = session.getPaymentIntent();
        if (StringUtils.hasText(paymentIntent)) {
            return paymentIntent;
        }
        return session.getId();
    }
}
