package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.CaptureAttemptPurchase;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptureAttemptPurchaseRepository
    extends JpaRepository<CaptureAttemptPurchase, Long> {

  Optional<CaptureAttemptPurchase> findByUserIdAndClientRequestId(
      Long userId, String clientRequestId);
}
