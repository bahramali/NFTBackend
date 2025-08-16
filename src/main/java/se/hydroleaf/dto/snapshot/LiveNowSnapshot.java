package se.hydroleaf.dto.snapshot;

import java.util.Map;

public record LiveNowSnapshot(
        Map<String, SystemSnapshot> systems
) {}
