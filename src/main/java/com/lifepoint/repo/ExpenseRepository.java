package com.lifepoint.repo;

import com.lifepoint.model.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByOwnerEmailOrderBySpentOnDesc(String email);
    List<Expense> findByOwnerEmailAndSpentOnBetween(String email, LocalDate start, LocalDate end);
    List<Expense> findByOwnerEmailAndCategory(String email, String category);
    Optional<Expense> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
