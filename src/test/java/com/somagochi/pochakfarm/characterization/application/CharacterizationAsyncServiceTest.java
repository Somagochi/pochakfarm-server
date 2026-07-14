package com.somagochi.pochakfarm.characterization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class CharacterizationAsyncServiceTest {

  private final CharacterizationRepository characterizationRepository =
      mock(CharacterizationRepository.class);
  private final CharacterizerClient characterizerClient = mock(CharacterizerClient.class);
  private final ImageUploadService imageUploadService = mock(ImageUploadService.class);
  private final CharacterizationAsyncService service =
      new CharacterizationAsyncService(
          characterizationRepository, characterizerClient, imageUploadService);

  @Test
  void completesCharacterizationBySavingOnlyResultImage() {
    Characterization characterization = processing();
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), eq(metadata())))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                12345));
    given(
            imageUploadService.uploadPublic(
                "characterization-result", "image/png", bytes("result-image")))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));

    service.characterizeAsync(1L, bytes("original-image"), "image/png", "솜구름", metadata());

    assertEquals(CharacterizationStatus.SUCCEEDED, characterization.getStatus());
    assertEquals("public/result.png", characterization.getResultImageKey());
    verify(imageUploadService, never()).uploadPublic(eq("characterization-back"), any(), any());
  }

  @Test
  void recordsFailureWhenCharacterizerThrows() {
    Characterization characterization = processing();
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), eq(metadata())))
        .willThrow(new BusinessException(ErrorCode.CHARACTERIZATION_FAILED));

    service.characterizeAsync(1L, bytes("original-image"), "image/png", "솜구름", metadata());

    assertEquals(CharacterizationStatus.FAILED, characterization.getStatus());
    assertEquals(ErrorCode.CHARACTERIZATION_FAILED.getCode(), characterization.getFailureReason());
  }

  @Test
  void recordsFailureWhenResultImageBase64IsInvalid() {
    Characterization characterization = processing();
    given(characterizationRepository.findById(1L)).willReturn(Optional.of(characterization));
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), eq(metadata())))
        .willReturn(
            new CharacterizerResult("success", "codex_exec", "image/png", "not-base64", 10));

    service.characterizeAsync(1L, bytes("original-image"), "image/png", "솜구름", metadata());

    assertEquals(CharacterizationStatus.FAILED, characterization.getStatus());
  }

  private static Characterization processing() {
    Characterization characterization = Characterization.start(1L, "솜구름", metadata());
    ReflectionTestUtils.setField(characterization, "id", 1L);
    return characterization;
  }

  private static CardMetadata metadata() {
    return new CardMetadata(
        CardType.SKY, 82, CardSkill.SKY_CLOUD_JUMP, CardSkill.SKY_WIND_DASH, "001");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
