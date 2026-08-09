package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.DailyCaptureAttempt;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyCaptureAttemptRepository extends JpaRepository<DailyCaptureAttempt, Long> {

  Optional<DailyCaptureAttempt> findByUserIdAndAttemptDate(Long userId, LocalDate attemptDate);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select attempt from DailyCaptureAttempt attempt "
          + "where attempt.userId = :userId and attempt.attemptDate = :attemptDate")
  Optional<DailyCaptureAttempt> findByUserIdAndAttemptDateForUpdate(
      @Param("userId") Long userId, @Param("attemptDate") LocalDate attemptDate);
}
