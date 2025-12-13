package se.hydroleaf.controller.dto;

import jakarta.validation.constraints.AssertTrue;
import se.hydroleaf.model.UserStatus;

public record AdminStatusUpdateRequest(Boolean active, UserStatus status) {

    @AssertTrue(message = "Either status or active must be provided")
    public boolean hasValue() {
        return active != null || status != null;
    }
}
