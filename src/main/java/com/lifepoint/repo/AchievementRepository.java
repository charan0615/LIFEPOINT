package com.lifepoint.repo;

import com.lifepoint.model.Achievement;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {
    List<Achievement> findByOwnerEmailOrderByUnlockedOnDesc(String ownerEmail);
    boolean existsByOwnerEmailAndBadgeCode(String ownerEmail, String badgeCode);
}
