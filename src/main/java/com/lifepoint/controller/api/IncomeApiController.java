package com.lifepoint.controller.api;

import com.lifepoint.dto.ApiResponse;
import com.lifepoint.dto.IncomeRequest;
import com.lifepoint.model.Income;
import com.lifepoint.repo.IncomeRepository;
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
@RequestMapping("/api/income")
public class IncomeApiController {
    private final LifePointService service;
    private final IncomeRepository incomeRepo;

    public IncomeApiController(LifePointService service, IncomeRepository incomeRepo) {
        this.service = service;
        this.incomeRepo = incomeRepo;
    }

    @GetMapping
    public ResponseEntity<List<Income>> getIncomes(
            Principal principal,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<Income> list = incomeRepo.findByOwnerEmailOrderByReceivedOnDesc(email);

        if (category != null && !category.isBlank() && !"ALL".equalsIgnoreCase(category)) {
            list = list.stream().filter(i -> category.equalsIgnoreCase(i.getCategory())).collect(Collectors.toList());
        }
        if (search != null && !search.isBlank()) {
            String q = search.toLowerCase().trim();
            list = list.stream().filter(i -> i.getTitle() != null && i.getTitle().toLowerCase().contains(q)).collect(Collectors.toList());
        }
        if (startDate != null) {
            list = list.stream().filter(i -> i.getReceivedOn() != null && !i.getReceivedOn().isBefore(startDate)).collect(Collectors.toList());
        }
        if (endDate != null) {
            list = list.stream().filter(i -> i.getReceivedOn() != null && !i.getReceivedOn().isAfter(endDate)).collect(Collectors.toList());
        }

        return ResponseEntity.ok(list);
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Income>> createIncome(Principal principal, @Valid @RequestBody IncomeRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDate receivedOn = (request.getReceivedOn() != null) ? request.getReceivedOn() : LocalDate.now();
        Income created = service.addIncome(email, request.getTitle().trim(), request.getCategory(), request.getAmount(), receivedOn);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Income logged! +10 XP", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Income>> updateIncome(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody IncomeRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        LocalDate receivedOn = (request.getReceivedOn() != null) ? request.getReceivedOn() : LocalDate.now();
        Income updated = service.updateIncome(email, id, request.getTitle().trim(), request.getCategory(), request.getAmount(), receivedOn);
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Income not found or unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Income updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteIncome(Principal principal, @PathVariable Long id) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        service.deleteIncome(email, id);
        return ResponseEntity.ok(ApiResponse.ok("Income deleted", null));
    }
}
