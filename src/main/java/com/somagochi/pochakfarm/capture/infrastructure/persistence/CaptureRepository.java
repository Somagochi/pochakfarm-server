package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.dto.CaptureCount;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaptureRepository extends JpaRepository<Capture, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select c from Capture c where c.id = :captureId")
  Optional<Capture> findByIdForUpdate(@Param("captureId") Long captureId);

  Optional<Capture> findByUserIdAndClientRequestId(Long userId, String clientRequestId);

  long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long userId, Instant startInclusive, Instant endExclusive);

  @Query(
      "select c from Capture c, Animal a "
          + "where a.id = :animalId and a.captureId = c.id and c.userId = :userId")
  Optional<Capture> findByUserIdAndAnimalId(
      @Param("userId") Long userId, @Param("animalId") Long animalId);

  @Query(
      "select new com.somagochi.pochakfarm.capture.dto.CaptureCount(c.cardType, c.tier, count(c)) "
          + "from Capture c "
          + "where c.userId = :userId and c.gameStatus = :gameStatus "
          + "group by c.cardType, c.tier")
  List<CaptureCount> countByCardTypeAndTier(
      @Param("userId") Long userId, @Param("gameStatus") GameStatus gameStatus);
}
