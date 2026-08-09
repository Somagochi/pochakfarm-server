package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CaptureQueryServiceTest {

  private final CaptureRepository captureRepository = mock(CaptureRepository.class);
  private final FileStorage fileStorage = mock(FileStorage.class);
  private final CaptureQueryService service =
      new CaptureQueryService(
          captureRepository,
          fileStorage,
          Clock.fixed(Instant.parse("2026-07-24T01:00:00Z"), ZoneOffset.UTC));

  @Test
  void returnsNullImageUrlsBeforeGenerationSucceeds() {
    Capture capture = capture();
    when(captureRepository.findById(123L)).thenReturn(Optional.of(capture));

    CaptureResponse response = service.getCapture(1L, 123L);

    assertNull(response.cardImageUrl());
    assertNull(response.animalImageUrl());
  }

  @Test
  void returnsImageUrlsWhenGenerationSucceeds() {
    Capture capture = capture();
    capture.succeed("public/capture-animal/1/123.png", "public/capture-card/card.png", 100);
    when(captureRepository.findById(123L)).thenReturn(Optional.of(capture));
    when(fileStorage.buildUrl("public/capture-card/card.png"))
        .thenReturn("https://cdn.test/card.png");
    when(fileStorage.buildUrl("public/capture-animal/1/123.png"))
        .thenReturn("https://cdn.test/animal.png");

    CaptureResponse response = service.getCapture(1L, 123L);

    assertEquals("https://cdn.test/card.png", response.cardImageUrl());
    assertEquals("https://cdn.test/animal.png", response.animalImageUrl());
  }

  @Test
  void showsEffectiveExpiredStatusWithoutChangingCapture() {
    Capture capture = capture();
    CaptureQueryService expiredService =
        new CaptureQueryService(
            captureRepository,
            fileStorage,
            Clock.fixed(Instant.parse("2026-07-24T01:05:00Z"), ZoneOffset.UTC));
    when(captureRepository.findById(123L)).thenReturn(Optional.of(capture));

    CaptureResponse response = expiredService.getCapture(1L, 123L);

    assertEquals(GameStatus.EXPIRED, response.gameStatus());
    assertEquals(GameStatus.PENDING, capture.getGameStatus());
  }

  private Capture capture() {
    Capture capture =
        Capture.create(
            1L,
            UUID.randomUUID().toString(),
            CardType.GROUND,
            Tier.B,
            AnimalName.from("두부"),
            CardSkill.GROUND_PAW_STRIKE,
            CardSkill.GROUND_LEAF_GUARD,
            "123",
            "images/capture-original/1/original.jpg",
            "image/jpeg",
            Instant.parse("2026-07-24T01:05:00Z"));
    ReflectionTestUtils.setField(capture, "id", 123L);
    return capture;
  }
}
