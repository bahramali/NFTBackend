package se.hydroleaf.controller.dto;

import se.hydroleaf.model.User;
import se.hydroleaf.model.UserRole;
import java.util.Arrays;
import java.util.List;
import se.hydroleaf.model.Permission;

public record MyProfileResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        String phoneNumber,
        UserRole role,
        List<String> roles,
        List<String> permissions,
        boolean orderConfirmationEmails,
        boolean pickupReadyNotification,
        NotificationPreferencesResponse notificationPreferences
) {

    public static MyProfileResponse from(User user) {
        List<String> roles = List.of(user.getRole().name());
        List<String> permissions = user.getRole() == UserRole.SUPER_ADMIN
                ? Arrays.stream(Permission.values()).map(Enum::name).toList()
                : user.getPermissions().stream().map(Enum::name).toList();
        return new MyProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getDisplayName(),
                user.getPhone(),
                user.getPhone(),
                user.getRole(),
                roles,
                permissions,
                user.isOrderConfirmationEmails(),
                user.isPickupReadyNotification(),
                new NotificationPreferencesResponse(
                        user.isOrderConfirmationEmails(),
                        user.isPickupReadyNotification()
                )
        );
    }
}
