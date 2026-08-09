package com.somagochi.pochakfarm.characterization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CharacterizationStatusServiceTest {

  private final CharacterizationRepository characterizationRepository =
      mock(CharacterizationRepository.class);
  private final CharacterizationStatusService service =
      new CharacterizationStatusService(characterizationRepository);

  @Test
  void marksCharacterizationSucceeded() {
    Characterization characterization = processing();
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));

    service.succeed(1L, "public/result.png", "codex_exec", 12345);

    assertEquals(CharacterizationStatus.SUCCEEDED, characterization.getStatus());
    assertEquals("public/result.png", characterization.getResultImageKey());
    assertEquals("codex_exec", characterization.getProvider());
    assertEquals(12345, characterization.getElapsedMs());
  }

  @Test
  void marksCharacterizationFailed() {
    Characterization characterization = processing();
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));

    service.fail(1L, "CHARACTERIZATION_FAILED");

    assertEquals(CharacterizationStatus.FAILED, characterization.getStatus());
    assertEquals("CHARACTERIZATION_FAILED", characterization.getFailureReason());
  }

  @Test
  void throwsNotFoundWhenCharacterizationMissing() {
    given(characterizationRepository.findById(999L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.fail(999L, "CHARACTERIZATION_FAILED"));

    assertEquals(ErrorCode.CHARACTERIZATION_NOT_FOUND.getCode(), exception.getCode());
  }

  private static Characterization processing() {
    Characterization characterization =
        Characterization.start(1L, AnimalName.from("솜구름"), metadata());
    ReflectionTestUtils.setField(characterization, "id", 1L);
    return characterization;
  }

  private static CardMetadata metadata() {
    return new CardMetadata(
        CardType.SKY, 82, CardSkill.SKY_CLOUD_JUMP, CardSkill.SKY_WIND_DASH, "001");
  }
}
