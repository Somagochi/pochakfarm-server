package com.somagochi.pochakfarm.farm.infrastructure.persistence;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FarmSpaceRepository extends JpaRepository<FarmSpace, Long> {

  Optional<FarmSpace> findByUserIdAndType(Long userId, CardType type);
}
