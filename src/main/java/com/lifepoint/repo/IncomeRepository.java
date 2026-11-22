package com.lifepoint.repo;

import com.lifepoint.model.Income;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IncomeRepository extends JpaRepository<Income, Long> {
    List<Income> findByOwnerEmailOrderByReceivedOnDesc(String ownerEmail);
    List<Income> findByOwnerEmailAndReceivedOnBetween(String ownerEmail, LocalDate start, LocalDate end);
    List<Income> findByOwnerEmailAndCategory(String ownerEmail, String category);
    Optional<Income> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
