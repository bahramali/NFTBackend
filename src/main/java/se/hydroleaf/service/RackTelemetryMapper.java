package se.hydroleaf.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class RackTelemetryMapper {

    private static final Pattern LEGACY_RACK_PATTERN = Pattern.compile("^RACK_(\\d+)$");

    public String resolveTelemetryRackId(String rackId, String telemetryRackId) {
        if (telemetryRackId != null) {
            return telemetryRackId;
        }
        if (rackId == null) {
            return null;
        }
        Matcher matcher = LEGACY_RACK_PATTERN.matcher(rackId);
        if (matcher.matches()) {
            return "R" + matcher.group(1);
        }
        return rackId;
    }
}
