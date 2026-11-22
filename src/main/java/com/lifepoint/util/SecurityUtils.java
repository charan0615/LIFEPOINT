package com.lifepoint.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.security.Principal;

public class SecurityUtils {
    public static String resolveEmail(Principal principal) {
        if (principal == null) return null;
        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User user = oauthToken.getPrincipal();
            String email = user.getAttribute("email");
            if (email != null && !email.isBlank()) return email;
        } else if (principal instanceof Authentication auth) {
            if (auth.getPrincipal() instanceof OAuth2User user) {
                String email = user.getAttribute("email");
                if (email != null && !email.isBlank()) return email;
            }
            return auth.getName();
        }
        return principal.getName();
    }

    public static String resolveName(Principal principal) {
        if (principal == null) return "Member";
        if (principal instanceof OAuth2AuthenticationToken oauthToken) {
            OAuth2User user = oauthToken.getPrincipal();
            String name = user.getAttribute("name");
            if (name != null && !name.isBlank()) return name;
        } else if (principal instanceof Authentication auth) {
            if (auth.getPrincipal() instanceof OAuth2User user) {
                String name = user.getAttribute("name");
                if (name != null && !name.isBlank()) return name;
            }
        }
        return "Member";
    }
}
