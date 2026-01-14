package se.hydroleaf.store.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import se.hydroleaf.store.model.Payment;
import se.hydroleaf.store.model.PaymentProvider;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;
import se.hydroleaf.store.repository.CartItemRepository;
import se.hydroleaf.store.repository.CartRepository;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.PaymentRepository;
import se.hydroleaf.store.repository.ProductRepository;
import se.hydroleaf.store.repository.ProductVariantRepository;
import se.hydroleaf.store.service.StripeService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private CartRepository cartRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @MockBean
    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void checkoutPersistsStripeProviderReference() throws Exception {
        Product product = productRepository.save(Product.builder()
                .sku("BASIL")
                .name("Fresh Basil")
                .description("Test basil")
                .priceCents(0)
                .currency("SEK")
                .active(true)
                .inventoryQty(0)
                .imageUrl("https://example.com/basil.png")
                .category("greens")
                .build());

        ProductVariant variant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .label("50g")
                .weightGrams(50)
                .priceCents(2900)
                .stockQuantity(10)
                .sku("BASIL-50G")
                .active(true)
                .build());

        when(stripeService.createCheckoutSession(any()))
                .thenReturn(new StripeService.StripeSessionResult("cs_test_123", "https://stripe.test/checkout"));

        MvcResult createCartResult = mockMvc.perform(post("/api/store/cart")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cartNode = objectMapper.readTree(createCartResult.getResponse().getContentAsByteArray());
        UUID cartId = UUID.fromString(cartNode.get("id").asText());

        String addItemRequest = """
                {
                  "variantId": "%s",
                  "quantity": 1
                }
                """.formatted(variant.getId());

        mockMvc.perform(post("/api/store/cart/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(addItemRequest))
                .andExpect(status().isOk());

        String checkoutRequest = """
                {
                  "cartId": "%s",
                  "email": "buyer@example.com",
                  "shippingAddress": {
                    "name": "Test Buyer",
                    "line1": "Street 1",
                    "city": "Stockholm",
                    "postalCode": "11122",
                    "country": "SE",
                    "phone": "123456"
                  }
                }
                """.formatted(cartId);

        mockMvc.perform(post("/api/store/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(checkoutRequest))
                .andExpect(status().isOk());

        List<Payment> payments = paymentRepository.findAll();
        assertThat(payments).hasSize(1);
        Payment payment = payments.get(0);
        assertThat(payment.getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(payment.getProviderReference()).isEqualTo("cs_test_123");
    }
}
