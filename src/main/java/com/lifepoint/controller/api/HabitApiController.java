package com.lifepoint.controller.api;

import com.lifepoint.dto.ApiResponse;
import com.lifepoint.dto.HabitRequest;
import com.lifepoint.model.Habit;
import com.lifepoint.repo.HabitRepository;
import com.lifepoint.service.LifePointService;
import com.lifepoint.util.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/habits")
public class HabitApiController {
    private final LifePointService service;
    private final HabitRepository habitRepo;

    public HabitApiController(LifePointService service, HabitRepository habitRepo) {
        this.service = service;
        this.habitRepo = habitRepo;
    }

    @GetMapping
    public ResponseEntity<List<Habit>> getHabits(Principal principal) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        return ResponseEntity.ok(habitRepo.findByOwnerEmail(email));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Habit>> createHabit(Principal principal, @Valid @RequestBody HabitRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Habit habit = service.addHabit(
                email,
                request.getTitle().trim(),
                request.getCategory(),
                request.isMoneySaver(),
                request.getSavedAmount(),
                request.getLinkedGoalId()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Habit created successfully! +10 XP", habit));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Habit>> updateHabit(
            Principal principal,
            @PathVariable Long id,
            @Valid @RequestBody HabitRequest request) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Habit updated = service.updateHabit(
                email,
                id,
                request.getTitle().trim(),
                request.getCategory(),
                request.isMoneySaver(),
                request.getSavedAmount(),
                request.getLinkedGoalId()
        );
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error("Habit not found or unauthorized"));
        }
        return ResponseEntity.ok(ApiResponse.ok("Habit updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHabit(Principal principal, @PathVariable Long id) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        service.deleteHabit(email, id);
        return ResponseEntity.ok(ApiResponse.ok("Habit deleted", null));
    }

    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<Habit>> completeHabit(Principal principal, @PathVariable Long id) {
        String email = SecurityUtils.resolveEmail(principal);
        if (email == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        boolean done = service.completeHabit(email, id);
        if (!done) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Habit already completed today or not found"));
        }

        Habit habit = habitRepo.findByIdAndOwnerEmail(id, email).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok("Habit completed! Streak extended and XP earned!", habit));
    }
}
