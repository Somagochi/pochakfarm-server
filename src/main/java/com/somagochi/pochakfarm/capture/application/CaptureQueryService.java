package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaptureQueryService {

  private final CaptureRepository captureRepository;

  public CaptureQueryService(CaptureRepository captureRepository) {
    this.captureRepository = captureRepository;
  }

  @Transactional(readOnly = true)
  public Optional<Capture> findById(Long captureId) {
    return captureRepository.findById(captureId);
  }
}
