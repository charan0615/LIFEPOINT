package com.lifepoint.repo;

import com.lifepoint.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByOwnerEmail(String ownerEmail);
    Optional<Budget> findByOwnerEmailAndCategory(String ownerEmail, String category);
    Optional<Budget> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
