package se.hydroleaf.store.web;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import se.hydroleaf.model.UserRole;
import se.hydroleaf.service.AuthenticatedUser;
import se.hydroleaf.service.AuthorizationService;
import se.hydroleaf.service.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void createProductReturnsPricingAndInventoryFields() throws Exception {
        given(authorizationService.requireAuthenticated(anyString()))
                .willReturn(new AuthenticatedUser(1L, UserRole.ADMIN, Set.of()));

        String requestBody = """
                {
                  "name": "Integration Test Product",
                  "description": "Created via integration test",
                  "price": 19.99,
                  "currency": "SEK",
                  "stock": 7,
                  "imageUrl": "https://example.com/image.png",
                  "category": "testing"
                }
                """;

        String accessToken = jwtService.createAccessToken(new AuthenticatedUser(1L, UserRole.ADMIN, Set.of()));

        MvcResult createResult = mockMvc.perform(post("/api/admin/store/products")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priceCents").value(1999))
                .andExpect(jsonPath("$.inventoryQty").value(7))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.stock").value(7))
                .andReturn();

        JsonNode createdProduct = objectMapper.readTree(createResult.getResponse().getContentAsByteArray());
        String productId = createdProduct.get("id").asText();

        mockMvc.perform(get("/api/admin/store/products/{id}", productId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(1999))
                .andExpect(jsonPath("$.inventoryQty").value(7))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.stock").value(7));
    }

    @Test
    void createVariantAndListProductsIncludesVariants() throws Exception {
        given(authorizationService.requireAuthenticated(anyString()))
                .willReturn(new AuthenticatedUser(1L, UserRole.ADMIN, Set.of()));

        String productRequest = """
                {
                  "name": "Variant Product",
                  "description": "Base product",
                  "price": 0,
                  "currency": "SEK",
                  "stock": 0,
                  "imageUrl": "https://example.com/variant.png",
                  "category": "testing"
                }
                """;

        String accessToken = jwtService.createAccessToken(new AuthenticatedUser(1L, UserRole.ADMIN, Set.of()));

        MvcResult createResult = mockMvc.perform(post("/api/admin/store/products")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdProduct = objectMapper.readTree(createResult.getResponse().getContentAsByteArray());
        String productId = createdProduct.get("id").asText();

        String variantRequest = """
                {
                  "weightGrams": 50,
                  "priceSek": 19.5,
                  "stockQuantity": 12,
                  "sku": "VAR-50",
                  "ean": "1234567890123",
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/admin/store/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.weightGrams").value(50))
                .andExpect(jsonPath("$.priceCents").value(1950))
                .andExpect(jsonPath("$.stockQuantity").value(12))
                .andExpect(jsonPath("$.sku").value("VAR-50"));

        mockMvc.perform(get("/api/admin/store/products/{id}", productId)
                        .param("includeVariants", "true")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variants[0].weightGrams").value(50))
                .andExpect(jsonPath("$.variants[0].priceCents").value(1950));
    }

    @Test
    void duplicateVariantWeightReturnsConflict() throws Exception {
        given(authorizationService.requireAuthenticated(anyString()))
                .willReturn(new AuthenticatedUser(1L, UserRole.ADMIN, Set.of()));

        String productRequest = """
                {
                  "name": "Duplicate Weight Product",
                  "description": "Base product",
                  "price": 0,
                  "currency": "SEK",
                  "stock": 0,
                  "imageUrl": "https://example.com/dup.png",
                  "category": "testing"
                }
                """;

        String accessToken = jwtService.createAccessToken(new AuthenticatedUser(1L, UserRole.ADMIN, Set.of()));

        MvcResult createResult = mockMvc.perform(post("/api/admin/store/products")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(productRequest))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdProduct = objectMapper.readTree(createResult.getResponse().getContentAsByteArray());
        String productId = createdProduct.get("id").asText();

        String variantRequest = """
                {
                  "weightGrams": 70,
                  "priceSek": 20,
                  "stockQuantity": 5,
                  "sku": "VAR-70",
                  "active": true
                }
                """;

        mockMvc.perform(post("/api/admin/store/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantRequest))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/store/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(variantRequest))
                .andExpect(status().isConflict());
    }
}
