package se.hydroleaf.controller.dto;

import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;

public record MyProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String phoneNumber,
        UserRole role,
        boolean orderConfirmationEmails,
        boolean pickupReadyNotification,
        NotificationPreferencesResponse notificationPreferences
) {

    public static MyProfileResponse from(User user) {
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhone(),
                user.getPhone(),
                user.getRole(),
                user.isOrderConfirmationEmails(),
                user.isPickupReadyNotification(),
                new NotificationPreferencesResponse(
                        user.isOrderConfirmationEmails(),
                        user.isPickupReadyNotification()
                )
        );
    }
}
