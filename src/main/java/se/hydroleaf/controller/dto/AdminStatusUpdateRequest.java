package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotNull;

public record AdminStatusUpdateRequest(@NotNull Boolean active) {
}
