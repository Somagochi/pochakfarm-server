package com.somagochi.pochakfarm.characterization.infrastructure.persistence;

import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CharacterizationRepository extends JpaRepository<Characterization, Long> {

  boolean existsByDeviceIdAndStatus(Long deviceId, CharacterizationStatus status);

  @Modifying(clearAutomatically = true)
  @Query(
      "update Characterization c "
          + "set c.status = :failedStatus, c.failureReason = :failureReason, c.updatedAt = :now "
          + "where c.status = :processingStatus and c.createdAt < :threshold")
  int failStaleProcessing(
      @Param("processingStatus") CharacterizationStatus processingStatus,
      @Param("failedStatus") CharacterizationStatus failedStatus,
      @Param("failureReason") String failureReason,
      @Param("threshold") Instant threshold,
      @Param("now") Instant now);
}
