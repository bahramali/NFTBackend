package se.hydroleaf.payments.stripe;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(StripePaymentController.class)
@ActiveProfiles("test")
class StripePaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StripePaymentService stripePaymentService;

    @Test
    @WithMockUser
    void createPaymentIntentReturnsClientSecret() throws Exception {
        StripePaymentService.StripePaymentIntentResult result =
                new StripePaymentService.StripePaymentIntentResult("pi_secret", "pi_123");
        when(stripePaymentService.createPaymentIntent(any(StripePaymentIntentRequest.class), anyString()))
                .thenReturn(result);

        mockMvc.perform(post("/api/payments/stripe/payment-intents")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" +
                                "\"amount\":2500," +
                                "\"currency\":\"sek\"," +
                                "\"orderId\":\"order-1\"" +
                                "}"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Idempotency-Key"))
                .andExpect(jsonPath("$.clientSecret").value("pi_secret"))
                .andExpect(jsonPath("$.paymentIntentId").value("pi_123"));
    }
}
