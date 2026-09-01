package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.GymLeaderClear;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymLeaderClearRepository extends JpaRepository<GymLeaderClear, Long> {

  Optional<GymLeaderClear> findByBattleId(Long battleId);

  boolean existsByUserIdAndGymLeaderId(Long userId, Long gymLeaderId);
}
