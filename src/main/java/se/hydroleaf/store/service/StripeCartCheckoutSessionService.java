package se.hydroleaf.store.service;

import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.hydroleaf.common.api.BadRequestException;
import se.hydroleaf.common.api.StripeIntegrationException;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.store.api.dto.MoneySummary;
import se.hydroleaf.store.api.dto.ShippingAddressDto;
import se.hydroleaf.store.api.dto.StripeCheckoutSessionRequest;
import se.hydroleaf.store.config.StoreProperties;
import se.hydroleaf.store.config.StripeProperties;
import se.hydroleaf.store.model.Cart;
import se.hydroleaf.store.model.CartItem;
import se.hydroleaf.store.model.PaymentAttempt;
import se.hydroleaf.store.model.PaymentAttemptStatus;
import se.hydroleaf.store.repository.PaymentAttemptRepository;

@Service
@RequiredArgsConstructor
public class StripeCartCheckoutSessionService {

    private static final Logger log = LoggerFactory.getLogger(StripeCartCheckoutSessionService.class);
    private static final String SUCCESS_URL =
            "https://hydroleaf.se/store/checkout/success?session_id={CHECKOUT_SESSION_ID}";
    private static final String CANCEL_URL = "https://hydroleaf.se/store/checkout/cancel";

    private final CartService cartService;
    private final StripeProperties stripeProperties;
    private final StoreProperties storeProperties;
    private final PaymentAttemptRepository paymentAttemptRepository;

    public StripeCheckoutSessionResult createCheckoutSession(
            AuthenticatedUser user,
            StripeCheckoutSessionRequest request
    ) {
        ensureStripeEnabled();

        CartService.CartCheckoutSnapshot snapshot = cartService.loadCartForCheckout(request.getCartId());
        Cart cart = snapshot.cart();
        MoneySummary totals = snapshot.totals();

        if (totals.getTotalCents() <= 0) {
            throw new BadRequestException("CART_TOTAL_INVALID", "Cart total must be greater than zero");
        }

        String currency = totals.getCurrency();
        if (!"SEK".equalsIgnoreCase(currency) || !currencyMatchesStore(currency)) {
            throw new BadRequestException("CART_CURRENCY_INVALID", "Cart currency must be SEK");
        }

        List<SessionCreateParams.LineItem> lineItems = buildLineItems(cart, totals);
        ShippingAddressDto shippingAddress = request.getShippingAddress();
        SessionCreateParams.PaymentIntentData paymentIntentData = SessionCreateParams.PaymentIntentData.builder()
                .putMetadata("userId", String.valueOf(user.userId()))
                .putMetadata("cartId", cart.getId().toString())
                .putMetadata("email", request.getEmail())
                .putMetadata("ship_name", nullToEmpty(shippingAddress.getName()))
                .putMetadata("ship_line1", nullToEmpty(shippingAddress.getLine1()))
                .putMetadata("ship_line2", nullToEmpty(shippingAddress.getLine2()))
                .putMetadata("ship_postalCode", nullToEmpty(shippingAddress.getPostalCode()))
                .putMetadata("ship_city", nullToEmpty(shippingAddress.getCity()))
                .putMetadata("ship_state", nullToEmpty(shippingAddress.getState()))
                .putMetadata("ship_country", nullToEmpty(shippingAddress.getCountry()))
                .putMetadata("ship_phone", nullToEmpty(shippingAddress.getPhone()))
                .build();
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(SUCCESS_URL)
                .setCancelUrl(CANCEL_URL)
                .addAllLineItem(lineItems)
                .setPaymentIntentData(paymentIntentData)
                .putMetadata("userId", String.valueOf(user.userId()))
                .putMetadata("cartId", cart.getId().toString())
                .putMetadata("email", request.getEmail())
                .putMetadata("ship_name", nullToEmpty(shippingAddress.getName()))
                .putMetadata("ship_line1", nullToEmpty(shippingAddress.getLine1()))
                .putMetadata("ship_line2", nullToEmpty(shippingAddress.getLine2()))
                .putMetadata("ship_postalCode", nullToEmpty(shippingAddress.getPostalCode()))
                .putMetadata("ship_city", nullToEmpty(shippingAddress.getCity()))
                .putMetadata("ship_state", nullToEmpty(shippingAddress.getState()))
                .putMetadata("ship_country", nullToEmpty(shippingAddress.getCountry()))
                .putMetadata("ship_phone", nullToEmpty(shippingAddress.getPhone()))
                .build();

        try {
            Session session = Session.create(params);
            paymentAttemptRepository.findByStripeSessionId(session.getId())
                    .orElseGet(() -> paymentAttemptRepository.save(PaymentAttempt.builder()
                            .stripeSessionId(session.getId())
                            .cartId(cart.getId())
                            .userId(cart.getUserId())
                            .status(PaymentAttemptStatus.CREATED)
                            .build()));
            log.info("Created Stripe checkout session {} for cartId={}", session.getId(), cart.getId());
            return new StripeCheckoutSessionResult(session.getUrl());
        } catch (StripeException e) {
            throw new StripeIntegrationException("Unable to create Stripe checkout session: " + e.getMessage());
        }
    }

    private List<SessionCreateParams.LineItem> buildLineItems(Cart cart, MoneySummary totals) {
        String currency = totals.getCurrency().toLowerCase(Locale.ROOT);
        List<SessionCreateParams.LineItem> lineItems = new ArrayList<>();
        for (CartItem item : cart.getItems()) {
            String name = resolveItemName(item);
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity((long) item.getQty())
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency)
                            .setUnitAmount(item.getUnitPriceCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName(name)
                                    .build())
                            .build())
                    .build());
        }

        if (totals.getShippingCents() > 0) {
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency)
                            .setUnitAmount(totals.getShippingCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Shipping")
                                    .build())
                            .build())
                    .build());
        }

        if (totals.getTaxCents() > 0) {
            lineItems.add(SessionCreateParams.LineItem.builder()
                    .setQuantity(1L)
                    .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                            .setCurrency(currency)
                            .setUnitAmount(totals.getTaxCents())
                            .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                    .setName("Tax")
                                    .build())
                            .build())
                    .build());
        }

        return lineItems;
    }

    private String resolveItemName(CartItem item) {
        String name = item.getProduct() != null ? item.getProduct().getName() : "Item";
        if (item.getVariant() != null && StringUtils.hasText(item.getVariant().getLabel())) {
            return name + " (" + item.getVariant().getLabel() + ")";
        }
        return name;
    }

    private boolean currencyMatchesStore(String currency) {
        return storeProperties.getCurrency() != null
                && storeProperties.getCurrency().equalsIgnoreCase(currency);
    }

    private void ensureStripeEnabled() {
        if (!stripeProperties.isEnabled() || !StringUtils.hasText(stripeProperties.getSecretKey())) {
            throw new BadRequestException("STRIPE_DISABLED", "Stripe payments are not configured.");
        }
        Stripe.apiKey = stripeProperties.getSecretKey();
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    public record StripeCheckoutSessionResult(String checkoutUrl) {}
}
