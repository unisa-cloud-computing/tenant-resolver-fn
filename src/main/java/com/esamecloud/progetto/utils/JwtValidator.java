package com.esamecloud.progetto.utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.SignedJWT;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTClaimsVerifier;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

public class JwtValidator {

    private static final ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
    private static final String ENTRA_TENANT_ID = "c7f158c6-9d4c-4789-ae4e-664fbdb8f405"; // tenant di External ID (GUID)
    private static final String expectedIssuer = "https://" + ENTRA_TENANT_ID + ".ciamlogin.com/" + ENTRA_TENANT_ID + "/v2.0";
    private static final String ENTRA_EXTENSION_APP_ID = "db48f4b5-121b-41fb-9d65-b4912aaeab2a"; // appId per la custom extension
    private static final Logger logger = Logger.getLogger(JwtValidator.class.getName());

    public static void validate(String token) throws Exception {
        JwtValidator.init();
        logger.info("init done, validating token");
        SignedJWT jwt = SignedJWT.parse(token);
        var claims = jwtProcessor.process(jwt, null);

        String iss = claims.getIssuer();
        List<String> aud = claims.getAudience();

        if (!expectedIssuer.equals(iss)) {
            throw new RuntimeException("Invalid issuer: " + iss);
        }
        if (aud == null || aud.isEmpty() || !aud.contains(ENTRA_EXTENSION_APP_ID)) {
            throw new RuntimeException("Invalid audience: " + aud);
        }
    }


    private static void init() throws Exception {

        String jwksUrl = "https://cloudsaasmanagement.ciamlogin.com/" + ENTRA_TENANT_ID +"/discovery/v2.0/keys";

        // Scarica le chiavi pubbliche (JWK) di Entra
        DefaultResourceRetriever resourceRetriever = new DefaultResourceRetriever(2000, 2000);
        RemoteJWKSet<SecurityContext> jwkSource = new RemoteJWKSet<>(new URL(jwksUrl), resourceRetriever);

        JWSKeySelector<SecurityContext> keySelector =
                new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);


        jwtProcessor.setJWSKeySelector(keySelector);

        // Verifica di base su iss, aud, exp
        jwtProcessor.setJWTClaimsSetVerifier(new DefaultJWTClaimsVerifier<>(
                null,
                new HashSet<>(Set.of("iss", "aud", "exp", "iat"))
        ));
    }
}

