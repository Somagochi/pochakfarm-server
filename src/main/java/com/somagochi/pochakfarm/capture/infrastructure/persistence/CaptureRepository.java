package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.Capture;
import java.time.Instant;
import java.util.Optional;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptureRepository extends JpaRepository<Capture, Long> {

  Optional<Capture> findByUserIdAndClientRequestId(Long userId, String clientRequestId);

  long countByUserIdAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
      Long userId, Instant startInclusive, Instant endExclusive);

  List<Capture> findByUserId(Long userId);

  List<Capture> findByUserIdAndCardType(Long userId, CardType cardType);
}
