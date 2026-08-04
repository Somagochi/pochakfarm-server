package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.dto.CaptureTypeCount;
import jakarta.persistence.LockModeType;
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

  @Query(
      "select c from Capture c, Animal a "
          + "where a.id = :animalId and a.captureId = c.id and c.userId = :userId")
  Optional<Capture> findByUserIdAndAnimalId(
      @Param("userId") Long userId, @Param("animalId") Long animalId);

  @Query(
      "select new com.somagochi.pochakfarm.capture.dto.CaptureTypeCount(c.cardType, count(c)) "
          + "from Capture c "
          + "where c.userId = :userId "
          + "and c.gameStatus = com.somagochi.pochakfarm.capture.domain.GameStatus.SUCCEEDED "
          + "group by c.cardType")
  List<CaptureTypeCount> countSucceededByCardType(@Param("userId") Long userId);
}
