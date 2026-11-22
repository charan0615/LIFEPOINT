package com.lifepoint.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
public class SecurityConfig {

    private final GoogleOAuthValidationFilter googleOAuthValidationFilter;

    public SecurityConfig(GoogleOAuthValidationFilter googleOAuthValidationFilter) {
        this.googleOAuthValidationFilter = googleOAuthValidationFilter;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(a -> a
                .requestMatchers(
                    "/",
                    "/index",
                    "/index.html",
                    "/demo",
                    "/auth/**",
                    "/css/**",
                    "/js/**",
                    "/images/**",
                    "/favicon.ico",
                    "/error",
                    "/ws/**"
                ).permitAll()
                .anyRequest().authenticated())
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers("/ws/**")
            )
            .addFilterBefore(googleOAuthValidationFilter, OAuth2AuthorizationRequestRedirectFilter.class)
            .oauth2Login(o -> o
                .loginPage("/")
                .defaultSuccessUrl("/dashboard", true)
                .failureHandler((request, response, exception) -> {
                    String msg = (exception != null && exception.getMessage() != null)
                            ? exception.getMessage()
                            : "Authentication failed";
                    response.sendRedirect("/?error=oauth_failure&message=" + URLEncoder.encode(msg, StandardCharsets.UTF_8));
                })
            )
            .logout(l -> l
                .logoutUrl("/logout")
                .logoutSuccessUrl("/")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .permitAll());

        return http.build();
    }
}
