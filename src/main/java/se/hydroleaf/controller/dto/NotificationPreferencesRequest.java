package se.hydroleaf.controller.dto;

public record NotificationPreferencesRequest(
        Boolean orderConfirmationEmails,
        Boolean pickupReadyNotification
) {
}
