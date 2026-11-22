package com.lifepoint.repo;

import com.lifepoint.model.Profile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {
    Optional<Profile> findByEmail(String email);
    List<Profile> findTop10ByOrderByXpDesc();
}
