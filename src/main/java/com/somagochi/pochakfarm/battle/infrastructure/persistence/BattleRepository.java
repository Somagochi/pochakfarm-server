package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattleStatus;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BattleRepository extends JpaRepository<Battle, Long> {

  Optional<Battle> findFirstByUserIdAndStatusOrderByStartedAtDesc(Long userId, BattleStatus status);

  Optional<Battle> findByUserIdAndClientRequestId(Long userId, String clientRequestId);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select b from Battle b where b.id = :battleId")
  Optional<Battle> findByIdForUpdate(@Param("battleId") Long battleId);
}
