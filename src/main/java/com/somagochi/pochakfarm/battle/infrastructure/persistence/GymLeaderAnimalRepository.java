package com.somagochi.pochakfarm.battle.infrastructure.persistence;

import com.somagochi.pochakfarm.battle.domain.GymLeaderAnimal;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GymLeaderAnimalRepository extends JpaRepository<GymLeaderAnimal, Long> {

  List<GymLeaderAnimal> findByGymLeaderIdOrderByOrderNoAsc(Long gymLeaderId);

  List<GymLeaderAnimal> findByGymLeaderIdInOrderByGymLeaderIdAscOrderNoAsc(
      Collection<Long> gymLeaderIds);
}
