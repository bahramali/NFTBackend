package se.hydroleaf.store.payment;

import java.util.Map;
import se.hydroleaf.store.model.StoreOrder;

public interface PaymentProviderClient {

    HostedCheckoutSession createHostedCheckoutSession(StoreOrder order, long amount, String currency, String successUrl, String cancelUrl);

    boolean verifyWebhookSignature(Map<String, String> headers, byte[] rawBody);

    WebhookEvent parseWebhookEvent(Map<String, String> headers, byte[] rawBody);

    record HostedCheckoutSession(String providerPaymentId, String redirectUrl, String providerReference) {}

    record WebhookEvent(String providerPaymentId, String status, String method, Long amount, String currency) {}
}
