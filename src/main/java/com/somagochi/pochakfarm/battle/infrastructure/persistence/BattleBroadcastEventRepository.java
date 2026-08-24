package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.BattleBroadcastEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleBroadcastEventRepository extends JpaRepository<BattleBroadcastEvent, Long> {

  List<BattleBroadcastEvent> findByBattleIdOrderByEventSeqAsc(Long battleId);

  List<BattleBroadcastEvent> findByBattleIdAndActionSeqOrderByEventSeqAsc(
      Long battleId, Integer actionSeq);
}
