package se.hydroleaf.repository.dto.snapshot;

import java.util.Map;

public record LiveNowSnapshot(
        Map<String, SystemSnapshot> systems
) {}
