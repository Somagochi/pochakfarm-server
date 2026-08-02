package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.Capture;
import jakarta.persistence.LockModeType;
import java.time.Instant;
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
      "select count(c) from Capture c "
          + "where c.userId = :userId "
          + "and (c.paymentType = com.somagochi.pochakfarm.capture.domain.CapturePaymentType.FREE "
          + "or c.paymentType is null) "
          + "and c.createdAt >= :startInclusive "
          + "and c.createdAt < :endExclusive")
  long countFreeAttemptsByUserIdBetween(
      @Param("userId") Long userId,
      @Param("startInclusive") Instant startInclusive,
      @Param("endExclusive") Instant endExclusive);

  @Query(
      "select c from Capture c, Animal a "
          + "where a.id = :animalId and a.captureId = c.id and c.userId = :userId")
  Optional<Capture> findByUserIdAndAnimalId(
      @Param("userId") Long userId, @Param("animalId") Long animalId);
}
