package se.hydroleaf.store.payment;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import se.hydroleaf.store.config.NetsEasyProperties;
import se.hydroleaf.store.model.StoreOrder;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

@Service
@RequiredArgsConstructor
public class NetsEasyClient implements PaymentProviderClient {

    private static final Logger log = LoggerFactory.getLogger(NetsEasyClient.class);

    private final NetsEasyProperties properties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public HostedCheckoutSession createHostedCheckoutSession(StoreOrder order, long amount, String currency, String successUrl, String cancelUrl) {
        NetsEasyCreatePaymentRequest request = NetsEasyCreatePaymentRequest.from(order, amount, currency, successUrl, cancelUrl);
        WebClient client = webClientBuilder.build();
        NetsEasyCreatePaymentResponse response = client.post()
                .uri(properties.getApiBaseUrl() + "/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.AUTHORIZATION, properties.getSecretKey())
                .bodyValue(request)
                .retrieve()
                .bodyToMono(NetsEasyCreatePaymentResponse.class)
                .block();

        if (response == null || !StringUtils.hasText(response.paymentId())) {
            throw new IllegalStateException("Nets Easy payment creation failed");
        }

        String redirectUrl = Optional.ofNullable(response.hostedPaymentPageUrl())
                .orElseThrow(() -> new IllegalStateException("Nets Easy hosted payment page URL missing"));

        log.info("Created Nets Easy payment orderId={} paymentId={}", order.getId(), response.paymentId());
        return new HostedCheckoutSession(response.paymentId(), redirectUrl, response.paymentReference());
    }

    @Override
    public boolean verifyWebhookSignature(Map<String, String> headers, byte[] rawBody) {
        String signatureHeader = getHeaderIgnoreCase(headers, "X-Nets-Signature")
                .or(() -> getHeaderIgnoreCase(headers, "X-Nets-Signature-HMAC"))
                .orElse(null);
        if (!StringUtils.hasText(properties.getWebhookSecret()) || !StringUtils.hasText(signatureHeader)) {
            log.warn("Missing Nets Easy webhook signature or secret");
            return false;
        }

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(properties.getWebhookSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(keySpec);
            byte[] digest = mac.doFinal(rawBody);
            String hex = HexFormat.of().formatHex(digest);
            String base64 = Base64.getEncoder().encodeToString(digest);
            return signatureHeader.equalsIgnoreCase(hex) || signatureHeader.equals(base64);
        } catch (Exception ex) {
            log.error("Failed to verify Nets Easy webhook signature", ex);
            return false;
        }
    }

    @Override
    public WebhookEvent parseWebhookEvent(Map<String, String> headers, byte[] rawBody) {
        try {
            JsonNode root = objectMapper.readTree(rawBody);
            String paymentId = readText(root, "paymentId")
                    .or(() -> readText(root, "id"))
                    .or(() -> readNestedText(root, "payment", "id"))
                    .or(() -> readNestedText(root, "payment", "paymentId"))
                    .orElse(null);
            String status = readText(root, "status")
                    .or(() -> readText(root, "event"))
                    .or(() -> readNestedText(root, "payment", "status"))
                    .orElse(null);
            String method = readText(root, "method")
                    .or(() -> readText(root, "paymentMethod"))
                    .or(() -> readNestedText(root, "payment", "method"))
                    .orElse(null);
            Long amount = readLong(root, "amount")
                    .or(() -> readNestedLong(root, "payment", "amount"))
                    .orElse(null);
            String currency = readText(root, "currency")
                    .or(() -> readNestedText(root, "payment", "currency"))
                    .orElse(null);
            return new WebhookEvent(paymentId, status, method, amount, currency);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Unable to parse Nets Easy webhook payload", ex);
        }
    }

    private Optional<String> getHeaderIgnoreCase(Map<String, String> headers, String key) {
        if (headers == null) {
            return Optional.empty();
        }
        return headers.entrySet().stream()
                .filter(entry -> entry.getKey() != null && entry.getKey().equalsIgnoreCase(key))
                .map(Map.Entry::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private Optional<String> readText(JsonNode node, String field) {
        if (node != null && node.hasNonNull(field)) {
            return Optional.ofNullable(node.get(field).asText(null));
        }
        return Optional.empty();
    }

    private Optional<String> readNestedText(JsonNode node, String parent, String field) {
        if (node != null && node.hasNonNull(parent)) {
            JsonNode child = node.get(parent);
            if (child.hasNonNull(field)) {
                return Optional.ofNullable(child.get(field).asText(null));
            }
        }
        return Optional.empty();
    }

    private Optional<Long> readLong(JsonNode node, String field) {
        if (node != null && node.hasNonNull(field)) {
            return Optional.of(node.get(field).asLong());
        }
        return Optional.empty();
    }

    private Optional<Long> readNestedLong(JsonNode node, String parent, String field) {
        if (node != null && node.hasNonNull(parent)) {
            JsonNode child = node.get(parent);
            if (child.hasNonNull(field)) {
                return Optional.of(child.get(field).asLong());
            }
        }
        return Optional.empty();
    }

    private record NetsEasyCreatePaymentRequest(
            Checkout checkout,
            Order order
    ) {
        static NetsEasyCreatePaymentRequest from(StoreOrder storeOrder, long amount, String currency, String successUrl, String cancelUrl) {
            return new NetsEasyCreatePaymentRequest(
                    new Checkout(successUrl, cancelUrl),
                    new Order(amount, currency, storeOrder.getOrderNumber(), storeOrder.getEmail())
            );
        }

        private record Checkout(String returnUrl, String cancelUrl) {}

        private record Order(long amount, String currency, String reference, String email) {}
    }

    private record NetsEasyCreatePaymentResponse(
            String paymentId,
            String hostedPaymentPageUrl,
            String paymentReference
    ) {}
}
