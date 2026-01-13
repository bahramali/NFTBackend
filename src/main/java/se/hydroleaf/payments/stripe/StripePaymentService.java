package se.hydroleaf.payments.stripe;

import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import se.hydroleaf.common.api.BadRequestException;

@Service
public class StripePaymentService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);

    private final StripePaymentProperties properties;

    public StripePaymentService(StripePaymentProperties properties) {
        this.properties = properties;
    }

    public StripePaymentIntentResult createPaymentIntent(StripePaymentIntentRequest request, String idempotencyKey)
            throws StripeException {
        if (!properties.isAllowClientAmount()) {
            // TODO: Replace client-provided amounts with server-side order totals once product pricing is wired.
            throw new BadRequestException(
                    "STRIPE_AMOUNT_NOT_ALLOWED",
                    "Client-provided amounts are disabled; use server-side order totals."
            );
        }
        if (request.amount() == null || request.amount() <= 0) {
            throw new BadRequestException("INVALID_AMOUNT", "Amount must be greater than zero.");
        }
        String currency = StringUtils.hasText(request.currency()) ? request.currency().toLowerCase() : "sek";

        PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                .setAmount(request.amount())
                .setCurrency(currency)
                .setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );

        if (StringUtils.hasText(request.description())) {
            paramsBuilder.setDescription(request.description());
        }

        Map<String, String> metadata = new HashMap<>();
        if (StringUtils.hasText(request.orderId())) {
            metadata.put("orderId", request.orderId());
        }
        if (StringUtils.hasText(request.customerEmail())) {
            metadata.put("customerEmail", request.customerEmail());
        }
        if (!metadata.isEmpty()) {
            paramsBuilder.putAllMetadata(metadata);
        }

        RequestOptions requestOptions = RequestOptions.builder()
                .setIdempotencyKey(idempotencyKey)
                .build();

        PaymentIntent paymentIntent = PaymentIntent.create(paramsBuilder.build(), requestOptions);
        log.info(
                "Created Stripe PaymentIntent {} for orderId={}, amount={}, currency={}",
                paymentIntent.getId(),
                request.orderId(),
                request.amount(),
                currency
        );

        return new StripePaymentIntentResult(paymentIntent.getClientSecret(), paymentIntent.getId());
    }

    public record StripePaymentIntentResult(String clientSecret, String paymentIntentId) {
    }
}
