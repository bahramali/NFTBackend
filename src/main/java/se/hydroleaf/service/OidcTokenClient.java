package se.hydroleaf.service;

import se.hydroleaf.model.OauthProvider;

public interface OidcTokenClient {

    OidcTokenResponse exchangeAuthorizationCode(
            OauthProvider provider,
            String code,
            String codeVerifier
    );
}
