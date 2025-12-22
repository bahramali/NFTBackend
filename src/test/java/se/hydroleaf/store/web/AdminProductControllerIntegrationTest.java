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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthorizationService authorizationService;

    @Test
    void createProductReturnsPricingAndInventoryFields() throws Exception {
        given(authorizationService.requireAdminOrOperator(anyString()))
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

        MvcResult createResult = mockMvc.perform(post("/api/admin/products")
                        .header("Authorization", "Bearer test-token")
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

        mockMvc.perform(get("/api/admin/products/{id}", productId)
                        .header("Authorization", "Bearer test-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceCents").value(1999))
                .andExpect(jsonPath("$.inventoryQty").value(7))
                .andExpect(jsonPath("$.price").value(19.99))
                .andExpect(jsonPath("$.stock").value(7));
    }
}
