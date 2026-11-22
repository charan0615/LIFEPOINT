package com.lifepoint.controller.api;

import com.lifepoint.dto.ApiResponse;
import com.lifepoint.dto.BudgetRequest;
import com.lifepoint.dto.BudgetStatus;
import com.lifepoint.model.Budget;
import com.lifepoint.service.LifePointService;
import com.lifepoint.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/budgets")
public class BudgetApiController {
    private final LifePointService service;

    public BudgetApiController(LifePointService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<BudgetStatus>> getBudgets(Principal principal) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(service.getBudgetStatuses(email));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Budget>> setBudget(Principal principal, @Valid @RequestBody BudgetRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Budget b = service.setBudget(email, request.getCategory().trim(), request.getMonthlyLimit());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Budget threshold saved", b));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Budget>> updateBudget(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody BudgetRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Budget updated = service.updateBudget(email, id, request.getMonthlyLimit());
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Budget not found or unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Budget updated", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteBudget(Principal principal, @PathVariable Long id) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        service.deleteBudget(email, id);
        return ResponseEntity.ok(ApiResponse.ok("Budget deleted", null));
    }
}
