package com.somagochi.pochakfarm.farm.infrastructure.persistence;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FarmSpaceRepository extends JpaRepository<FarmSpace, Long> {

  Optional<FarmSpace> findByUserIdAndType(Long userId, CardType type);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT space FROM FarmSpace space WHERE space.userId = :userId AND space.type = :type")
  Optional<FarmSpace> findByUserIdAndTypeForUpdate(
      @Param("userId") Long userId, @Param("type") CardType type);
}
