package com.lifepoint.repo;

import com.lifepoint.model.Habit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HabitRepository extends JpaRepository<Habit, Long> {
    List<Habit> findByOwnerEmail(String email);
    List<Habit> findByOwnerEmailOrderByCompletedOnDesc(String email);
    Optional<Habit> findByIdAndOwnerEmail(Long id, String ownerEmail);
}
