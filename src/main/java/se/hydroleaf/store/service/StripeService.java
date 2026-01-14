package se.hydroleaf.store.service;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.hydroleaf.common.api.StripeIntegrationException;
import se.hydroleaf.store.config.StripeProperties;
import se.hydroleaf.store.model.OrderItem;
import se.hydroleaf.store.model.StoreOrder;

@Service
@RequiredArgsConstructor
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    private final StripeProperties stripeProperties;

    public StripeSessionResult createCheckoutSession(StoreOrder order) {
        if (!stripeProperties.isEnabled() || !StringUtils.hasText(stripeProperties.getSecretKey())) {
            return null;
        }

        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            SessionCreateParams.LineItem line = SessionCreateParams.LineItem.builder()
                    .setQuantity((long) item.getQty())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency().toLowerCase())
                            .setUnitAmount(item.getUnitPriceCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(item.getNameSnapshot())
                                    .build())
                            .build())
                    .build();
            lineItems.add(line);
        }

        if (order.getShippingCents() > 0) {
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(order.getCurrency().toLowerCase())
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
                            .setCurrency(order.getCurrency().toLowerCase())
                            .setUnitAmount(order.getTaxCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Tax")
                                    .build())
                            .build())
                    .build());
        }

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(formatOrderUrl(stripeProperties.getSuccessUrl(), order.getId().toString()))
                .setCancelUrl(formatOrderUrl(stripeProperties.getCancelUrl(), order.getId().toString()))
                .addAllLineItem(lineItems)
                .putMetadata("orderId", order.getId().toString())
                .putMetadata("orderNumber", order.getOrderNumber())
                .setCustomerEmail(order.getEmail())
                .build();

        try {
            Session session = Session.create(params);
            log.info("Created Stripe checkout session {} for orderId={}", session.getId(), order.getId());
            return new StripeSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new StripeIntegrationException("Unable to create Stripe checkout session: " + e.getMessage());
        }
    }

    public StripeWebhookEvent extractWebhookEvent(String payload, String signatureHeader) {
        Event event = parseEvent(payload, signatureHeader);
        if (event == null) {
            return null;
        }

        EventDataObjectDeserializer deserializer = event.getDataObjectDeserializer();
        Optional<Object> object = deserializer.getObject();
        Session session = object.filter(Session.class::isInstance)
                .map(Session.class::cast)
                .orElse(null);
        PaymentIntent paymentIntent = object.filter(PaymentIntent.class::isInstance)
                .map(PaymentIntent.class::cast)
                .orElse(null);
        return new StripeWebhookEvent(event.getType(), session, paymentIntent);
    }

    private Event parseEvent(String payload, String signatureHeader) {
        try {
            if (!StringUtils.hasText(stripeProperties.getWebhookSecret())) {
                log.warn("Stripe webhook secret not configured; rejecting webhook event.");
                throw new StripeIntegrationException("Stripe webhook secret is not configured");
            }
            if (!StringUtils.hasText(signatureHeader)) {
                log.warn("Stripe webhook signature header missing; rejecting webhook event.");
                throw new StripeIntegrationException("Stripe webhook signature is missing");
            }
            Event event = Webhook.constructEvent(payload, signatureHeader, stripeProperties.getWebhookSecret());
            log.info("Stripe webhook event received id={} type={}", event.getId(), event.getType());
            return event;
        } catch (SignatureVerificationException e) {
            log.warn("Stripe signature verification failed: {}", e.getMessage());
            throw new StripeIntegrationException("Invalid Stripe webhook signature");
        }
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

    public record StripeSessionResult(String sessionId, String url) {}

    public record StripeWebhookEvent(String type, Session session, PaymentIntent paymentIntent) {}
}
