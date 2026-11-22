package com.lifepoint.repo;

import com.lifepoint.model.HabitLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;

public interface HabitLogRepository extends JpaRepository<HabitLog, Long> {
    List<HabitLog> findByOwnerEmail(String ownerEmail);
    List<HabitLog> findByOwnerEmailAndCompletedDateBetween(String ownerEmail, LocalDate start, LocalDate end);
    List<HabitLog> findByOwnerEmailOrderByCompletedDateDesc(String ownerEmail);
}
