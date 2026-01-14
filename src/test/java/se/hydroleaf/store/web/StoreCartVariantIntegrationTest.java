package se.hydroleaf.store.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import se.hydroleaf.store.model.Product;
import se.hydroleaf.store.model.ProductVariant;
import se.hydroleaf.store.repository.CartItemRepository;
import se.hydroleaf.store.repository.CartRepository;
import se.hydroleaf.store.repository.OrderRepository;
import se.hydroleaf.store.repository.ProductRepository;
import se.hydroleaf.store.repository.ProductVariantRepository;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StoreCartVariantIntegrationTest {

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

    @BeforeEach
    void setUp() {
        cartItemRepository.deleteAll();
        cartRepository.deleteAll();
        orderRepository.deleteAll();
        productVariantRepository.deleteAll();
        productRepository.deleteAll();
    }

    @Test
    void listProductsIncludesVariants() throws Exception {
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

        productVariantRepository.saveAll(List.of(
                ProductVariant.builder()
                        .product(product)
                        .label("50g")
                        .weightGrams(50)
                        .priceCents(2900)
                        .stockQuantity(10)
                        .sku("BASIL-50G")
                        .active(true)
                        .build(),
                ProductVariant.builder()
                        .product(product)
                        .label("70g")
                        .weightGrams(70)
                        .priceCents(3500)
                        .stockQuantity(5)
                        .sku("BASIL-70G")
                        .active(true)
                        .build()));

        mockMvc.perform(get("/api/store/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].variants[0].label").value("50g"))
                .andExpect(jsonPath("$[0].variants[0].weightGrams").value(50))
                .andExpect(jsonPath("$[0].variants[0].stockQuantity").value(10));
    }

    @Test
    void addToCartUsesVariantIdAndRejectsOutOfStock() throws Exception {
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

        ProductVariant inStockVariant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .label("50g")
                .weightGrams(50)
                .priceCents(2900)
                .stockQuantity(10)
                .sku("BASIL-50G")
                .active(true)
                .build());

        ProductVariant outOfStockVariant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .label("100g")
                .weightGrams(100)
                .priceCents(4500)
                .stockQuantity(0)
                .sku("BASIL-100G")
                .active(true)
                .build());

        MvcResult createCartResult = mockMvc.perform(post("/api/store/cart")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cartNode = objectMapper.readTree(createCartResult.getResponse().getContentAsByteArray());
        UUID cartId = UUID.fromString(cartNode.get("id").asText());

        String requestBody = """
                {
                  "variantId": "%s",
                  "quantity": 2
                }
                """.formatted(inStockVariant.getId());

        mockMvc.perform(post("/api/store/cart/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].variantId").value(inStockVariant.getId().toString()))
                .andExpect(jsonPath("$.items[0].variantLabel").value("50g"))
                .andExpect(jsonPath("$.items[0].weightGrams").value(50))
                .andExpect(jsonPath("$.items[0].unitPriceSek").value(29.00));

        String outOfStockRequest = """
                {
                  "variantId": "%s",
                  "quantity": 1
                }
                """.formatted(outOfStockVariant.getId());

        mockMvc.perform(post("/api/store/cart/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(outOfStockRequest))
                .andExpect(status().isConflict());
    }

    @Test
    void addToCartAcceptsLegacyItemId() throws Exception {
        Product product = productRepository.save(Product.builder()
                .sku("MINT")
                .name("Fresh Mint")
                .description("Test mint")
                .priceCents(0)
                .currency("SEK")
                .active(true)
                .inventoryQty(0)
                .imageUrl("https://example.com/mint.png")
                .category("greens")
                .build());

        ProductVariant inStockVariant = productVariantRepository.save(ProductVariant.builder()
                .product(product)
                .label("10g")
                .weightGrams(10)
                .priceCents(1500)
                .stockQuantity(8)
                .sku("MINT-10G")
                .active(true)
                .build());

        MvcResult createCartResult = mockMvc.perform(post("/api/store/cart")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cartNode = objectMapper.readTree(createCartResult.getResponse().getContentAsByteArray());
        UUID cartId = UUID.fromString(cartNode.get("id").asText());

        String requestBody = """
                {
                  "itemId": "%s",
                  "quantity": 1
                }
                """.formatted(inStockVariant.getId());

        mockMvc.perform(post("/api/store/cart/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].variantId").value(inStockVariant.getId().toString()))
                .andExpect(jsonPath("$.items[0].variantLabel").value("10g"))
                .andExpect(jsonPath("$.items[0].weightGrams").value(10));
    }

    @Test
    void addToCartReturnsValidationErrorForMissingVariant() throws Exception {
        MvcResult createCartResult = mockMvc.perform(post("/api/store/cart")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode cartNode = objectMapper.readTree(createCartResult.getResponse().getContentAsByteArray());
        UUID cartId = UUID.fromString(cartNode.get("id").asText());

        String requestBody = """
                {
                  "quantity": 1
                }
                """;

        mockMvc.perform(post("/api/store/cart/{cartId}/items", cartId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("variantId is required"));
    }
}
