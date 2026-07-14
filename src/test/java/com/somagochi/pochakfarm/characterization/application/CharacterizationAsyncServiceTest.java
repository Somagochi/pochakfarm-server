package com.somagochi.pochakfarm.characterization.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class CharacterizationAsyncServiceTest {

  private final CharacterizerClient characterizerClient = mock(CharacterizerClient.class);
  private final ImageUploadService imageUploadService = mock(ImageUploadService.class);
  private final CharacterizationStatusService characterizationStatusService =
      mock(CharacterizationStatusService.class);
  private final CharacterizationAsyncService service =
      new CharacterizationAsyncService(
          characterizerClient, imageUploadService, characterizationStatusService);

  @Test
  void completesCharacterizationBySavingOnlyResultImage() {
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

    verify(characterizationStatusService).succeed(1L, "public/result.png", "codex_exec", 12345);
    verify(characterizationStatusService, never()).fail(any(), any());
    verify(imageUploadService, never()).uploadPublic(eq("characterization-back"), any(), any());
  }

  @Test
  void recordsFailureWhenCharacterizerThrows() {
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), eq(metadata())))
        .willThrow(new BusinessException(ErrorCode.CHARACTERIZATION_FAILED));

    service.characterizeAsync(1L, bytes("original-image"), "image/png", "솜구름", metadata());

    verify(characterizationStatusService).fail(1L, ErrorCode.CHARACTERIZATION_FAILED.getCode());
  }

  @Test
  void recordsFailureWhenResultImageBase64IsInvalid() {
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), eq(metadata())))
        .willReturn(
            new CharacterizerResult("success", "codex_exec", "image/png", "not-base64", 10));

    service.characterizeAsync(1L, bytes("original-image"), "image/png", "솜구름", metadata());

    verify(characterizationStatusService).fail(1L, ErrorCode.CHARACTERIZATION_FAILED.getCode());
  }

  private static CardMetadata metadata() {
    return new CardMetadata(
        CardType.SKY, 82, CardSkill.SKY_CLOUD_JUMP, CardSkill.SKY_WIND_DASH, "001");
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
