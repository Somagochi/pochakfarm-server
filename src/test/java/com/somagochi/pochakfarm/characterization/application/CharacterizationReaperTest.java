package com.somagochi.pochakfarm.characterization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CharacterizationProperties;
import com.somagochi.pochakfarm.common.properties.CharacterizationProperties.Reaper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CharacterizationReaperTest {

  private static final Duration STALE_THRESHOLD = Duration.ofMinutes(15);

  private final CharacterizationRepository characterizationRepository =
      mock(CharacterizationRepository.class);
  private final CharacterizationReaper reaper =
      new CharacterizationReaper(
          characterizationRepository,
          new CharacterizationProperties(
              false, new Reaper(true, Duration.ofMinutes(1), STALE_THRESHOLD)));

  @Test
  void marksProcessingOlderThanThresholdAsTimedOut() {
    given(characterizationRepository.failStaleProcessing(any(), any(), any(), any(), any()))
        .willReturn(3);

    reaper.reapStaleProcessing();

    ArgumentCaptor<Instant> thresholdCaptor = ArgumentCaptor.forClass(Instant.class);
    ArgumentCaptor<Instant> nowCaptor = ArgumentCaptor.forClass(Instant.class);
    verify(characterizationRepository)
        .failStaleProcessing(
            eq(CharacterizationStatus.PROCESSING),
            eq(CharacterizationStatus.FAILED),
            eq(ErrorCode.CHARACTERIZATION_TIMED_OUT.getCode()),
            thresholdCaptor.capture(),
            nowCaptor.capture());
    assertEquals(
        STALE_THRESHOLD,
        Duration.between(thresholdCaptor.getValue(), nowCaptor.getValue()),
        "threshold는 실행 시각에서 stale-threshold만큼 이전이어야 한다");
  }

  @Test
  void handlesEmptyResultWithoutError() {
    given(characterizationRepository.failStaleProcessing(any(), any(), any(), any(), any()))
        .willReturn(0);

    reaper.reapStaleProcessing();

    verify(characterizationRepository)
        .failStaleProcessing(
            eq(CharacterizationStatus.PROCESSING),
            eq(CharacterizationStatus.FAILED),
            eq(ErrorCode.CHARACTERIZATION_TIMED_OUT.getCode()),
            any(),
            any());
  }
}
