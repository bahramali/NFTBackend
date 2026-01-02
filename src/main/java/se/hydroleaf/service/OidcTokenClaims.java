package se.hydroleaf.service;

public record OidcTokenClaims(
        String subject,
        String email,
        boolean emailVerified,
        String name,
        String pictureUrl
) {
}
