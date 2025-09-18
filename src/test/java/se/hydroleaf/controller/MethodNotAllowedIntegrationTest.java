package se.hydroleaf.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import se.hydroleaf.service.DeviceService;
import se.hydroleaf.service.RecordService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({RecordController.class, DeviceController.class})
@WithMockUser
class MethodNotAllowedIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecordService recordService;

    @MockBean
    private DeviceService deviceService;

    @Test
    void putHistoryAggregatedReturns405() throws Exception {
        mockMvc.perform(put("/api/records/history/aggregated"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void postDevicesReturns405() throws Exception {
        mockMvc.perform(post("/api/devices"))
                .andExpect(status().isMethodNotAllowed());
    }
}

