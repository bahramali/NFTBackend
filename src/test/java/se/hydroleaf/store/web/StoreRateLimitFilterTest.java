package se.hydroleaf.store.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import se.hydroleaf.common.api.ApiError;
import se.hydroleaf.store.config.StoreProperties;

import static org.assertj.core.api.Assertions.assertThat;

class StoreRateLimitFilterTest {

    private StoreRateLimitFilter filter;
    private StoreProperties storeProperties;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().findAndRegisterModules();
        storeProperties = new StoreProperties();
        storeProperties.getRateLimit().setCapacity(1);
        storeProperties.getRateLimit().setRefillTokens(1);
        storeProperties.getRateLimit().setRefillSeconds(60);

        filter = new StoreRateLimitFilter(storeProperties, objectMapper);
    }

    @Test
    void allowsRequestWhenWithinLimit() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/store/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(request, response, markInvoked(invoked));

        assertThat(invoked).isTrue();
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void returnsTooManyRequestsWhenLimitExceeded() throws ServletException, IOException {
        MockHttpServletRequest firstRequest = new MockHttpServletRequest("GET", "/api/store/products");
        MockHttpServletResponse firstResponse = new MockHttpServletResponse();
        filter.doFilter(firstRequest, firstResponse, (req, res) -> {});

        MockHttpServletRequest limitedRequest = new MockHttpServletRequest("GET", "/api/store/products");
        MockHttpServletResponse limitedResponse = new MockHttpServletResponse();
        AtomicBoolean invoked = new AtomicBoolean(false);

        filter.doFilter(limitedRequest, limitedResponse, markInvoked(invoked));

        assertThat(invoked).isFalse();
        assertThat(limitedResponse.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
        assertThat(limitedResponse.getContentType()).startsWith(MediaType.APPLICATION_JSON_VALUE);
        assertThat(limitedResponse.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");

        var error = objectMapper.readTree(limitedResponse.getContentAsByteArray());
        assertThat(error.get("code").asText()).isEqualTo("RATE_LIMITED");
        assertThat(error.get("message").asText()).isEqualTo("Too many requests, please slow down");
    }

    private FilterChain markInvoked(AtomicBoolean invoked) {
        return (request, response) -> invoked.set(true);
    }
}
