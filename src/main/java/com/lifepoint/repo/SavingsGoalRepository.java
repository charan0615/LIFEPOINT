package com.lifepoint.repo;

import com.lifepoint.model.SavingsGoal;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SavingsGoalRepository extends JpaRepository<SavingsGoal, Long> {
    List<SavingsGoal> findByOwnerEmailOrderByTargetDateAsc(String ownerEmail);
    Optional<SavingsGoal> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
