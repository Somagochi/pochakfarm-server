package com.somagochi.pochakfarm.capture.infrastructure.persistence;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CaptureRepository extends JpaRepository<Capture, Long> {

  List<Capture> findByUserId(Long userId);

  List<Capture> findByUserIdAndCardType(Long userId, CardType cardType);
}
