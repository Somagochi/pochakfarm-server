package com.somagochi.pochakfarm.characterization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CharacterizationReadServiceTest {

  private final CharacterizationRepository characterizationRepository =
      mock(CharacterizationRepository.class);
  private final FileStorage fileStorage = mock(FileStorage.class);
  private final CharacterizationReadService service =
      new CharacterizationReadService(characterizationRepository, fileStorage);

  @Test
  void returnsResultUrlForSucceededCharacterization() {
    Characterization characterization = succeeded("front-key");
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));
    given(fileStorage.buildUrl("front-key")).willReturn("https://cdn.test/front.png");

    CharacterizationResponse response = service.getCharacterization(1L);

    assertEquals(1L, response.characterizationId());
    assertEquals(CharacterizationStatus.SUCCEEDED, response.status());
    assertEquals("https://cdn.test/front.png", response.resultImageUrl());
    assertNull(response.failureReason());
  }

  @Test
  void returnsProcessingStatusWithoutResultUrl() {
    Characterization characterization = processing();
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));

    CharacterizationResponse response = service.getCharacterization(1L);

    assertEquals(CharacterizationStatus.PROCESSING, response.status());
    assertNull(response.resultImageUrl());
    assertNull(response.failureReason());
  }

  @Test
  void returnsFailedStatusAndFailureReason() {
    Characterization characterization = processing();
    characterization.fail("CHARACTERIZATION_FAILED");
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));

    CharacterizationResponse response = service.getCharacterization(1L);

    assertEquals(CharacterizationStatus.FAILED, response.status());
    assertNull(response.resultImageUrl());
    assertEquals("CHARACTERIZATION_FAILED", response.failureReason());
  }

  @Test
  void throwsNotFoundWhenCharacterizationMissing() {
    given(characterizationRepository.findById(999L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.getCharacterization(999L));
    assertEquals(ErrorCode.CHARACTERIZATION_NOT_FOUND.getCode(), exception.getCode());
  }

  private static Characterization succeeded(String resultImageKey) {
    CardMetadata metadata =
        new CardMetadata(
            CardType.SKY, 82, CardSkill.SKY_CLOUD_JUMP, CardSkill.SKY_WIND_DASH, "001");
    Characterization characterization =
        Characterization.start(1L, AnimalName.from("솜구름"), metadata);
    characterization.succeed(resultImageKey, "test-provider", 100);
    ReflectionTestUtils.setField(characterization, "id", 1L);
    return characterization;
  }

  private static Characterization processing() {
    CardMetadata metadata =
        new CardMetadata(
            CardType.SKY, 82, CardSkill.SKY_CLOUD_JUMP, CardSkill.SKY_WIND_DASH, "001");
    Characterization characterization =
        Characterization.start(1L, AnimalName.from("솜구름"), metadata);
    ReflectionTestUtils.setField(characterization, "id", 1L);
    return characterization;
  }
}
