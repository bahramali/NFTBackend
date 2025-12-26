package se.hydroleaf.controller.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MyProfileRequest(
        @Size(max = 128) String fullName,
        @JsonAlias("phone")
        @Size(max = 32)
        @Pattern(
                regexp = "^[0-9+()\\-\\s]{0,32}$",
                message = "Phone may only include digits, spaces, +, -, and parentheses"
        )
        String phoneNumber,
        Boolean orderConfirmationEmails,
        Boolean pickupReadyNotification,
        @Valid NotificationPreferencesRequest notificationPreferences
) {
}
