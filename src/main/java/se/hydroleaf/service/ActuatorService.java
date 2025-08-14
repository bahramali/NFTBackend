package se.hydroleaf.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import se.hydroleaf.model.Device;
import se.hydroleaf.model.OxygenPumpStatus;
import se.hydroleaf.repository.DeviceRepository;
import se.hydroleaf.repository.OxygenPumpStatusRepository;

import java.time.Instant;
import java.util.Objects;

/**
 * ActuatorService
 * - Saves oxygen pump status aligned with Option-1 (Device PK = composite_id).
 * - Required fields in payload:
 *    - composite_id (or compositeId)
 *    - controllers[]: array with entries {name:"airPump", state:true/false, timestamp?}
 * - Optional:
 *    - timestamp: ISO-8601 or epoch millis; defaults to now() and used when controller timestamp absent
 */
@Service
public class ActuatorService {

    private final ObjectMapper objectMapper;
    private final OxygenPumpStatusRepository pumpRepo;
    private final DeviceRepository deviceRepo;

    public ActuatorService(ObjectMapper objectMapper,
                           OxygenPumpStatusRepository pumpRepo,
                           DeviceRepository deviceRepo) {
        this.objectMapper = objectMapper;
        this.pumpRepo = pumpRepo;
        this.deviceRepo = deviceRepo;
    }

    /** Entry point used by tests and other layers. */
    @Transactional
    public void saveOxygenPumpStatus(String jsonString) {
        Objects.requireNonNull(jsonString, "payload is null");
        try {
            JsonNode node = objectMapper.readTree(jsonString);

            // 1) Composite ID (required)
            String compositeId = readCompositeId(node);
            if (compositeId == null || compositeId.isBlank()) {
                throw new IllegalArgumentException("composite_id is required in payload");
            }

            Device device = deviceRepo.findById(compositeId)
                    .orElseThrow(() -> new IllegalArgumentException("Unknown device composite_id: " + compositeId));

            // 2) Base timestamp (optional)
            Instant baseTs = readTimestamp(node);

            // 3) Controllers array with airPump entry
            JsonNode controllers = node.path("controllers");
            boolean savedAny = false;
            if (controllers.isArray()) {
                for (JsonNode c : controllers) {
                    String name = c.path("name").asText(null);
                    if (name != null && name.equalsIgnoreCase("airPump")) {
                        Boolean status = readStatus(c.path("state"));
                        if (status == null) continue;
                        Instant ts = c.hasNonNull("timestamp") ? readTimestamp(c) : baseTs;

                        OxygenPumpStatus row = new OxygenPumpStatus();
                        row.setDevice(device);   // FK via composite_id
                        row.setTimestamp(ts);
                        row.setStatus(status);
                        pumpRepo.save(row);
                        savedAny = true;
                    }
                }
            }

            if (!savedAny) {
                throw new IllegalArgumentException("controllers array with airPump state is required");
            }

        } catch (IllegalArgumentException iae) {
            throw iae;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse and save oxygen pump status", e);
        }
    }

    // -------- helpers --------

    private static String readCompositeId(JsonNode node) {
        if (node.hasNonNull("composite_id")) return node.get("composite_id").asText();
        if (node.hasNonNull("compositeId"))  return node.get("compositeId").asText();
        return null;
    }

    private static Instant readTimestamp(JsonNode node) {
        JsonNode t = node.path("timestamp");
        if (t.isMissingNode() || t.isNull()) return Instant.now();

        if (t.isNumber()) {
            try { return Instant.ofEpochMilli(t.asLong()); } catch (Exception ignore) {}
        }
        if (t.isTextual()) {
            try { return Instant.parse(t.asText()); } catch (Exception ignore) {}
        }
        // Fallback: now
        return Instant.now();
    }

    /** Accepts "on"/"off" (case-insensitive), true/false, 1/0. */
    private static Boolean readStatus(JsonNode n) {
        if (n == null || n.isNull() || n.isMissingNode()) return null;
        if (n.isBoolean()) return n.asBoolean();
        if (n.isInt() || n.isLong()) return n.asInt() != 0;
        if (n.isTextual()) {
            String s = n.asText().trim().toLowerCase();
            if ("on".equals(s) || "true".equals(s) || "1".equals(s)) return true;
            if ("off".equals(s) || "false".equals(s) || "0".equals(s)) return false;
        }
        return null;
    }
}
