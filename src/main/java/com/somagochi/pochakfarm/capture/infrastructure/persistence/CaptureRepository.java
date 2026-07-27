package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.Capture;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaptureRepository extends JpaRepository<Capture, Long> {

  @Query(
      "select c from Capture c, Animal a "
          + "where a.id = :animalId and a.captureId = c.id and c.userId = :userId")
  Optional<Capture> findByUserIdAndAnimalId(
      @Param("userId") Long userId, @Param("animalId") Long animalId);
}
