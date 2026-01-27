package se.hydroleaf.repository.dto.report;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;

public record DeviceEventResponse(
        String compositeId,
        Instant eventTime,
        String level,
        String code,
        String msg,
        JsonNode raw
) {
}
