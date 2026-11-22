package com.lifepoint.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class GoogleOAuthValidationFilter extends OncePerRequestFilter {

    private final GoogleOAuthConfig googleOAuthConfig;

    public GoogleOAuthValidationFilter(GoogleOAuthConfig googleOAuthConfig) {
        this.googleOAuthConfig = googleOAuthConfig;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        if (path != null && path.startsWith("/oauth2/authorization/google")) {
            if (!googleOAuthConfig.isConfigured()) {
                // Prevent silent redirection to Google with placeholder credentials (which triggers Google 401 invalid_client)
                response.sendRedirect("/?error=oauth_missing_credentials");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
