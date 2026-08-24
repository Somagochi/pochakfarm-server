package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.BattleAction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleActionRepository extends JpaRepository<BattleAction, Long> {

  Optional<BattleAction> findByBattleIdAndActionSeq(Long battleId, Integer actionSeq);

  Optional<BattleAction> findFirstByBattleIdOrderByActionSeqDesc(Long battleId);

  List<BattleAction> findByBattleIdOrderByActionSeqAsc(Long battleId);
}
