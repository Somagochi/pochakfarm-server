package com.somagochi.pochakfarm.characterization.application;

import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CharacterizationProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Slf4j
@ConditionalOnProperty(
    prefix = "app.characterization.reaper",
    name = "enabled",
    havingValue = "true")
public class CharacterizationReaper {

  private final CharacterizationRepository characterizationRepository;
  private final Duration staleThreshold;

  public CharacterizationReaper(
      CharacterizationRepository characterizationRepository,
      CharacterizationProperties properties) {
    this.characterizationRepository = characterizationRepository;
    this.staleThreshold = properties.reaper().staleThreshold();
  }

  @Scheduled(fixedDelayString = "${app.characterization.reaper.interval}")
  @Transactional
  public void reapStaleProcessing() {
    Instant now = Instant.now();
    Instant threshold = now.minus(staleThreshold);
    int reaped =
        characterizationRepository.failStaleProcessing(
            CharacterizationStatus.PROCESSING,
            CharacterizationStatus.FAILED,
            ErrorCode.CHARACTERIZATION_TIMED_OUT.getCode(),
            threshold,
            now);
    if (reaped > 0) {
      log.warn(
          "characterization_stale_reaped count={} olderThan={} threshold={}",
          reaped,
          threshold,
          staleThreshold);
    }
  }
}
