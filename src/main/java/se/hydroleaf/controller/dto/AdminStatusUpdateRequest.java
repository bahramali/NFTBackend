package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.NotNull;
import se.hydroleaf.model.UserStatus;

public record AdminStatusUpdateRequest(@NotNull UserStatus status) {
}
