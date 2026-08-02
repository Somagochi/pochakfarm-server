package com.somagochi.pochakfarm.achievement.infrastructure.persistence;

import com.somagochi.pochakfarm.achievement.domain.Achievement;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AchievementRepository extends JpaRepository<Achievement, Long> {

  Optional<Achievement> findByCode(String code);
}
