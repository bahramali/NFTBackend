package se.hydroleaf.controller.dto;

import java.time.LocalDateTime;

public record InviteValidationResponse(String email, String displayName, LocalDateTime expiresAt) {
}
