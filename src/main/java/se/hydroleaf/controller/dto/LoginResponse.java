package se.hydroleaf.controller.dto;

import java.util.List;
import se.hydroleaf.model.UserRole;

public record LoginResponse(
        Long userId,
        UserRole role,
        List<String> permissions,
        String token
) {
}
