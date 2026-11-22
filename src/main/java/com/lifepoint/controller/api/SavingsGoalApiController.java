package com.lifepoint.controller.api;

import com.lifepoint.dto.ApiResponse;
import com.lifepoint.dto.DepositRequest;
import com.lifepoint.dto.SavingsGoalRequest;
import com.lifepoint.model.SavingsGoal;
import com.lifepoint.repo.SavingsGoalRepository;
import com.lifepoint.service.LifePointService;
import com.lifepoint.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/goals")
public class SavingsGoalApiController {
    private final LifePointService service;
    private final SavingsGoalRepository goalRepo;

    public SavingsGoalApiController(LifePointService service, SavingsGoalRepository goalRepo) {
        this.service = service;
        this.goalRepo = goalRepo;
    }

    @GetMapping
    public ResponseEntity<List<SavingsGoal>> getGoals(Principal principal) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(goalRepo.findByOwnerEmailOrderByTargetDateAsc(email));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SavingsGoal>> createGoal(
            Principal principal,
            @Valid @RequestBody SavingsGoalRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDate date = (request.getTargetDate() != null) ? request.getTargetDate() : LocalDate.now().plusMonths(6);
        SavingsGoal goal = service.addSavingsGoal(
                email,
                request.getTitle().trim(),
                request.getTargetAmount(),
                date,
                request.getCategory(),
                request.getColor()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Savings goal created! +15 XP", goal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SavingsGoal>> updateGoal(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody SavingsGoalRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDate date = (request.getTargetDate() != null) ? request.getTargetDate() : LocalDate.now().plusMonths(6);
        SavingsGoal updated = service.updateSavingsGoal(
                email,
                id,
                request.getTitle().trim(),
                request.getTargetAmount(),
                date,
                request.getCategory(),
                request.getColor()
        );
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Goal not found or unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Savings goal updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteGoal(Principal principal, @PathVariable Long id) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        service.deleteSavingsGoal(email, id);
        return ResponseEntity.ok(ApiResponse.ok("Savings goal deleted", null));
    }

    @PostMapping("/{id}/deposit")
    public ResponseEntity<ApiResponse<SavingsGoal>> deposit(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody DepositRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        service.depositToSavingsGoal(email, id, request.getAmount());
        SavingsGoal goal = goalRepo.findByIdAndOwnerEmail(id, email).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("Deposited into savings vault! +20 XP", goal));
    }
}
