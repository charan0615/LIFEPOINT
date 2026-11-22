package com.lifepoint.controller.api;

import com.lifepoint.dto.ApiResponse;
import com.lifepoint.dto.ExpenseRequest;
import com.lifepoint.model.Expense;
import com.lifepoint.repo.ExpenseRepository;
import com.lifepoint.service.LifePointService;
import com.lifepoint.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseApiController {
    private final LifePointService service;
    private final ExpenseRepository expenseRepo;

    public ExpenseApiController(LifePointService service, ExpenseRepository expenseRepo) {
        this.service = service;
        this.expenseRepo = expenseRepo;
    }

    @GetMapping
    public ResponseEntity<List<Expense>> getExpenses(
            Principal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Expense> list = expenseRepo.findByOwnerEmailOrderBySpentOnDesc(email);

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            list = list.stream().filter(e -> category.equalsIgnoreCase(e.getCategory())).collect(Collectors.toList());
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase().trim();
            list = list.stream().filter(e -> e.getTitle() != null && e.getTitle().toLowerCase().contains(q)).collect(Collectors.toList());
        }
        if (startDate != null) {
            list = list.stream().filter(e -> e.getSpentOn() != null && !e.getSpentOn().isBefore(startDate)).collect(Collectors.toList());
        }
        if (endDate != null) {
            list = list.stream().filter(e -> e.getSpentOn() != null && !e.getSpentOn().isAfter(endDate)).collect(Collectors.toList());
        }

        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Expense>> createExpense(Principal principal, @Valid @RequestBody ExpenseRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDate spentOn = (request.getSpentOn() != null) ? request.getSpentOn() : LocalDate.now();
        Expense created = service.addExpense(email, request.getTitle().trim(), request.getCategory(), request.getAmount(), spentOn);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Expense recorded. +5 XP", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Expense>> updateExpense(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody ExpenseRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDate spentOn = (request.getSpentOn() != null) ? request.getSpentOn() : LocalDate.now();
        Expense updated = service.updateExpense(email, id, request.getTitle().trim(), request.getCategory(), request.getAmount(), spentOn);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Expense not found or unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Expense updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(Principal principal, @PathVariable Long id) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        service.deleteExpense(email, id);
        return ResponseEntity.ok(ApiResponse.ok("Expense deleted", null));
    }
}
