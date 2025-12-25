package se.hydroleaf.controller.dto;

import se.hydroleaf.model.User;

public record MyProfileResponse(
        String email,
        String fullName,
        String phone,
        NotificationPreferencesResponse notificationPreferences
) {

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getEmail(),
                user.getDisplayName(),
                user.getPhone(),
                new NotificationPreferencesResponse(
                        user.isOrderConfirmationEmails(),
                        user.isPickupReadyNotification()
                )
        );
    }
}
