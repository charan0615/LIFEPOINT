package com.lifepoint.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class GoogleOAuthConfig {
    private static final Logger log = LoggerFactory.getLogger(GoogleOAuthConfig.class);

    @Value("${spring.security.oauth2.client.registration.google.client-id:}")
    private String clientId;

    @Value("${spring.security.oauth2.client.registration.google.client-secret:}")
    private String clientSecret;

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:http://localhost:8080/login/oauth2/code/google}")
    private String redirectUri;

    @PostConstruct
    public void validateCredentials() {
        if (!isConfigured()) {
            log.warn("==========================================================================================");
            log.warn("[LIFEPOINT] GOOGLE OAUTH 2.0: Missing or placeholder credentials!");
            log.warn("Environment variables GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET are not configured.");
            log.warn("Placeholder values will NOT be sent to Google to prevent HTTP 401 (invalid_client) errors.");
            log.warn("To configure Google Sign-In, set environment variables with Google Cloud Console credentials:");
            log.warn("  GOOGLE_CLIENT_ID = <your-client-id>.apps.googleusercontent.com");
            log.warn("  GOOGLE_CLIENT_SECRET = <your-client-secret>");
            log.warn("  Authorized Redirect URI: http://localhost:8080/login/oauth2/code/google");
            log.warn("Local Faculty / Instant Access authentication (/demo and /auth/quick-login) is fully active.");
            log.warn("==========================================================================================");
        } else {
            log.info("[LIFEPOINT] Google OAuth 2.0 credentials successfully loaded from environment variables.");
        }
    }

    public boolean isConfigured() {
        if (clientId == null || clientSecret == null) return false;
        String cleanId = clientId.trim().toLowerCase();
        String cleanSecret = clientSecret.trim().toLowerCase();
        if (cleanId.isEmpty() || cleanSecret.isEmpty()) return false;
        if (cleanId.contains("placeholder") || cleanId.contains("unconfigured") || cleanId.contains("your-client-id") || cleanId.contains("test-client-id")) return false;
        if (cleanSecret.contains("placeholder") || cleanSecret.contains("unconfigured") || cleanSecret.contains("your-client-secret") || cleanSecret.contains("test-client-secret")) return false;
        return true;
    }

    public String getClientId() { return clientId; }
    public String getRedirectUri() { return redirectUri; }
}
