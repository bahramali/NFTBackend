package se.hydroleaf.dto;

import java.util.Map;

public record LiveNowSnapshot(
        Map<String, SystemSnapshot> systems
) {}
