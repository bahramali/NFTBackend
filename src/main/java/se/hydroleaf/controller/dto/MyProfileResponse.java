package se.hydroleaf.controller.dto;

import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;

public record MyProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        UserRole role,
        NotificationPreferencesResponse notificationPreferences
) {

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhone(),
                user.getRole(),
                new NotificationPreferencesResponse(
                        user.isOrderConfirmationEmails(),
                        user.isPickupReadyNotification()
                )
        );
    }
}
