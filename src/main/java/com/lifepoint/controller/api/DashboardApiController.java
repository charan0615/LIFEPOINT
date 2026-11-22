package com.lifepoint.controller.api;

import com.lifepoint.dto.ApiResponse;
import com.lifepoint.dto.DashboardSummary;
import com.lifepoint.dto.LeaderboardEntry;
import com.lifepoint.service.LifePointService;
import com.lifepoint.util.SecurityUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {
    private final LifePointService service;

    public DashboardApiController(LifePointService service) {
        this.service = service;
    }

    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummary>> getSummary(Principal principal) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        DashboardSummary summary = service.getDashboardSummary(email);
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<List<LeaderboardEntry>>> getLeaderboard() {
        return ResponseEntity.ok(ApiResponse.ok(service.getLeaderboard()));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics(Principal principal) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        return ResponseEntity.ok(ApiResponse.ok(service.getAnalytics(email)));
    }
}
