package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleRepository extends JpaRepository<Battle, Long> {

  Optional<Battle> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId, BattleStatus status);
}
