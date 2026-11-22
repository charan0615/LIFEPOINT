package com.lifepoint.controller.api;

import com.lifepoint.dto.ApiResponse;
import com.lifepoint.dto.CurrencyRequest;
import com.lifepoint.model.Profile;
import com.lifepoint.service.LifePointService;
import com.lifepoint.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Set;

@RestController
@RequestMapping("/api/profile")
public class ProfileApiController {
    private final LifePointService service;
    private static final Set<String> SUPPORTED_CURRENCIES = Set.of(
            "INR", "USD", "EUR", "GBP", "AED", "CAD", "AUD", "JPY", "SGD"
    );

    public ProfileApiController(LifePointService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Profile>> getProfile(Principal principal) {
        String email = SecurityUtils.resolveEmail(principal);
        String name = SecurityUtils.resolveName(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Profile profile = service.getOrCreateProfile(email, name);
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PostMapping("/currency")
    public ResponseEntity<ApiResponse<String>> updateCurrency(
            Principal principal,
            @Valid @RequestBody CurrencyRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        String code = request.getCurrency().trim().toUpperCase();
        if (!SUPPORTED_CURRENCIES.contains(code)) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Unsupported currency code: " + code));
        }

        service.updateCurrency(email, code);
        return ResponseEntity.ok(ApiResponse.ok("Currency updated to " + code, code));
    }
}
