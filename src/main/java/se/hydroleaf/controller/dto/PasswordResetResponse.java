package se.hydroleaf.controller.dto;

public record PasswordResetResponse(String message) {
    public static PasswordResetResponse success() {
        return new PasswordResetResponse("If the account exists, a password reset email will be sent.");
    }
}
