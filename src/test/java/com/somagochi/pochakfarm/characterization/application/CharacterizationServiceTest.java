package com.somagochi.pochakfarm.characterization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

class CharacterizationServiceTest {

  private final CharacterizationRepository characterizationRepository =
      mock(CharacterizationRepository.class);
  private final CharacterizerClient characterizerClient = mock(CharacterizerClient.class);
  private final ImageUploadService imageUploadService = mock(ImageUploadService.class);
  private final CharacterizationService service =
      new CharacterizationService(
          characterizationRepository, characterizerClient, imageUploadService);

  @Test
  void succeedsBySavingOriginalCallingCharacterizerAndSavingResult() {
    MockMultipartFile image = image("animal.png", "image/png", "original-image");
    given(
            imageUploadService.uploadPublic(
                "characterization-original", "image/png", bytes("original-image")))
        .willReturn(
            new PublicUploadResponse("public/original.png", "https://cdn.test/original.png"));
    given(characterizerClient.characterize(image, "솜구름"))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                null,
                "솜구름",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                12345));
    given(
            imageUploadService.uploadPublic(
                "characterization-result", "image/png", bytes("result-image")))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));

    CharacterizationResponse response = service.characterize(1L, image, " 솜구름 ");

    assertEquals("https://cdn.test/result.png", response.resultImageUrl());
    assertEquals("codex_exec", response.provider());
    assertEquals(12345, response.elapsedMs());
    ArgumentCaptor<Characterization> captor = ArgumentCaptor.forClass(Characterization.class);
    verify(characterizationRepository, times(2)).save(captor.capture());
    Characterization saved = lastCaptured(captor);
    assertEquals(1L, saved.getDeviceId());
    assertEquals("솜구름", saved.getAnimalName());
    assertEquals("public/original.png", saved.getOriginalImageKey());
    assertEquals("public/result.png", saved.getResultImageKey());
    assertEquals(CharacterizationStatus.SUCCEEDED, saved.getStatus());
  }

  @Test
  void rejectsWhenDeviceAlreadyHasSucceededCharacterization() {
    given(
            characterizationRepository.existsByDeviceIdAndStatus(
                1L, CharacterizationStatus.SUCCEEDED))
        .willReturn(true);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.characterize(1L, image(), "솜구름"));

    assertEquals(ErrorCode.CHARACTERIZATION_ALREADY_USED.getCode(), exception.getCode());
  }

  @Test
  void rejectsWhenDeviceHasProcessingCharacterization() {
    given(
            characterizationRepository.existsByDeviceIdAndStatus(
                1L, CharacterizationStatus.PROCESSING))
        .willReturn(true);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.characterize(1L, image(), "솜구름"));

    assertEquals(ErrorCode.CHARACTERIZATION_ALREADY_PROCESSING.getCode(), exception.getCode());
  }

  @Test
  void allowsRetryWhenDeviceHasOnlyFailedCharacterizations() {
    given(imageUploadService.uploadPublic(eq("characterization-original"), eq("image/png"), any()))
        .willReturn(
            new PublicUploadResponse("public/original.png", "https://cdn.test/original.png"));
    given(characterizerClient.characterize(any(), eq("솜구름")))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                null,
                "솜구름",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                10));
    given(imageUploadService.uploadPublic(eq("characterization-result"), eq("image/png"), any()))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));

    CharacterizationResponse response = service.characterize(1L, image(), "솜구름");

    assertEquals("https://cdn.test/result.png", response.resultImageUrl());
  }

  @Test
  void recordsFailedWhenCharacterizerThrows() {
    given(imageUploadService.uploadPublic(eq("characterization-original"), eq("image/png"), any()))
        .willReturn(
            new PublicUploadResponse("public/original.png", "https://cdn.test/original.png"));
    given(characterizerClient.characterize(any(), eq("솜구름")))
        .willThrow(new BusinessException(ErrorCode.CHARACTERIZATION_FAILED));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.characterize(1L, image(), "솜구름"));

    assertEquals(ErrorCode.CHARACTERIZATION_FAILED.getCode(), exception.getCode());
    ArgumentCaptor<Characterization> captor = ArgumentCaptor.forClass(Characterization.class);
    verify(characterizationRepository, times(2)).save(captor.capture());
    Characterization saved = lastCaptured(captor);
    assertEquals(CharacterizationStatus.FAILED, saved.getStatus());
    assertEquals(ErrorCode.CHARACTERIZATION_FAILED.getCode(), saved.getFailureReason());
  }

  @Test
  void rejectsInvalidResultBase64AndRecordsFailed() {
    given(imageUploadService.uploadPublic(eq("characterization-original"), eq("image/png"), any()))
        .willReturn(
            new PublicUploadResponse("public/original.png", "https://cdn.test/original.png"));
    given(characterizerClient.characterize(any(), eq("솜구름")))
        .willReturn(
            new CharacterizerResult(
                "success", "codex_exec", null, "솜구름", "image/png", "not-base64", 10));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.characterize(1L, image(), "솜구름"));

    assertEquals(ErrorCode.CHARACTERIZATION_FAILED.getCode(), exception.getCode());
    ArgumentCaptor<Characterization> captor = ArgumentCaptor.forClass(Characterization.class);
    verify(characterizationRepository, times(2)).save(captor.capture());
    assertEquals(CharacterizationStatus.FAILED, lastCaptured(captor).getStatus());
  }

  private static Characterization lastCaptured(ArgumentCaptor<Characterization> captor) {
    return captor.getAllValues().get(captor.getAllValues().size() - 1);
  }

  private static MockMultipartFile image() {
    return image("animal.png", "image/png", "original-image");
  }

  private static MockMultipartFile image(String filename, String contentType, String content) {
    return new MockMultipartFile("image", filename, contentType, bytes(content));
  }

  private static byte[] bytes(String value) {
    return value.getBytes(StandardCharsets.UTF_8);
  }
}
