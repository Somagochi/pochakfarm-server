package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CaptureProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "app.capture.reaper", name = "enabled", havingValue = "true")
public class CaptureReaper {

  private final CaptureRepository captureRepository;
  private final TransactionTemplate transactionTemplate;
  private final Duration staleThreshold;

  public CaptureReaper(
      CaptureRepository captureRepository,
      TransactionTemplate transactionTemplate,
      CaptureProperties properties) {
    this.captureRepository = captureRepository;
    this.transactionTemplate = transactionTemplate;
    this.staleThreshold = properties.reaper().staleThreshold();
  }

  @Scheduled(fixedDelayString = "${app.capture.reaper.interval}")
  public void reapStaleProcessing() {
    Instant threshold = Instant.now().minus(staleThreshold);
    List<Long> captureIds =
        captureRepository.findStaleProcessingIds(GenerationStatus.PROCESSING, threshold);
    AtomicInteger reaped = new AtomicInteger();
    for (Long captureId : captureIds) {
      transactionTemplate.executeWithoutResult(
          status ->
              captureRepository
                  .findByIdForUpdate(captureId)
                  .filter(capture -> capture.isProcessingStartedBefore(threshold))
                  .ifPresent(
                      capture -> {
                        capture.fail(ErrorCode.CHARACTERIZATION_TIMED_OUT.getCode());
                        reaped.incrementAndGet();
                      }));
    }
    if (reaped.get() > 0) {
      log.warn(
          "capture_stale_reaped count={} olderThan={} threshold={}",
          reaped.get(),
          threshold,
          staleThreshold);
    }
  }
}
