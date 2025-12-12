package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.Positive;

public record ResendInviteRequest(@Positive Integer expiresInHours) {
}
