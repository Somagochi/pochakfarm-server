package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.BattleEntry;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BattleEntryRepository extends JpaRepository<BattleEntry, Long> {

  List<BattleEntry> findByBattleIdOrderBySideAscOrderNoAsc(Long battleId);

  Optional<BattleEntry> findByBattleIdAndSideAndOrderNo(
      Long battleId, BattleSide side, Integer orderNo);
}
