package se.hydroleaf.controller.dto;

public record NotificationPreferencesResponse(
        boolean orderConfirmationEmails,
        boolean pickupReadyNotification
) {
}
