package se.hydroleaf.service;

import se.hydroleaf.model.OauthProvider;

public interface OidcTokenVerifier {

    OidcTokenClaims verifyIdToken(OauthProvider provider, String idToken, String nonce);
}
