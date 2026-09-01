package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.GymLeader;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymLeaderRepository extends JpaRepository<GymLeader, Long> {

  Optional<GymLeader> findByCode(String code);

  Optional<GymLeader> findByChallengeOrder(Integer challengeOrder);

  List<GymLeader> findAllByOrderByChallengeOrderAsc();
}
