package com.somagochi.pochakfarm.characterization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardMetadataGenerator;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CharacterizationProperties;
import com.somagochi.pochakfarm.storage.application.ImageUploadService;
import com.somagochi.pochakfarm.storage.dto.PublicUploadResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class CharacterizationServiceTest {

  private final CharacterizationRepository characterizationRepository =
      mock(CharacterizationRepository.class);
  private final CharacterizerClient characterizerClient = mock(CharacterizerClient.class);
  private final CardMetadataGenerator cardMetadataGenerator = mock(CardMetadataGenerator.class);
  private final ImageUploadService imageUploadService = mock(ImageUploadService.class);
  private final CharacterizationService service =
      new CharacterizationService(
          characterizationRepository,
          characterizerClient,
          cardMetadataGenerator,
          imageUploadService,
          new CharacterizationProperties(true));

  @Test
  void succeedsByPassingOriginalImageBase64AndSavingOnlyCardImages() {
    MockMultipartFile image = image("animal.png", "image/png", "original-image");
    CardMetadata metadata = metadata();
    given(cardMetadataGenerator.generate()).willReturn(metadata);
    given(
            characterizerClient.characterize(
                Base64.getEncoder().encodeToString(bytes("original-image")),
                "image/png",
                "솜구름",
                metadata))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                Base64.getEncoder().encodeToString(bytes("back-image")),
                12345));
    given(
            imageUploadService.uploadPublic(
                "characterization-result", "image/png", bytes("result-image")))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));
    given(
            imageUploadService.uploadPublic(
                "characterization-back", "image/png", bytes("back-image")))
        .willReturn(new PublicUploadResponse("public/back.png", "https://cdn.test/back.png"));

    CharacterizationResponse response = service.characterize(1L, image, " 솜구름 ");

    assertEquals("https://cdn.test/result.png", response.resultImageUrl());
    assertEquals("https://cdn.test/back.png", response.cardBackImageUrl());
    verify(imageUploadService, never()).uploadPublic(eq("characterization-original"), any(), any());
    verify(imageUploadService, never()).uploadPublic(eq("characterization-ai"), any(), any());
    ArgumentCaptor<Characterization> captor = ArgumentCaptor.forClass(Characterization.class);
    verify(characterizationRepository, times(2)).save(captor.capture());
    Characterization saved = lastCaptured(captor);
    assertEquals(1L, saved.getDeviceId());
    assertEquals("솜구름", saved.getAnimalName());
    assertEquals(CardType.SKY, saved.getCardType());
    assertEquals(82, saved.getPower());
    assertEquals(CardSkill.SKY_CLOUD_JUMP, saved.getSkill1());
    assertEquals(CardSkill.SKY_WIND_DASH, saved.getSkill2());
    assertEquals("001", saved.getCardNo());
    assertEquals("public/result.png", saved.getResultImageKey());
    assertEquals("public/back.png", saved.getCardBackImageKey());
    assertEquals(CharacterizationStatus.SUCCEEDED, saved.getStatus());
  }

  @Test
  void usesCharacterizationIdAsCyclicCardNumberBeforeCallingCharacterizer() {
    MockMultipartFile image = image("animal.png", "image/png", "original-image");
    CardMetadata generatedMetadata = metadata("999");
    given(cardMetadataGenerator.generate()).willReturn(generatedMetadata);
    given(characterizationRepository.save(any(Characterization.class)))
        .willAnswer(
            invocation -> {
              Characterization characterization = invocation.getArgument(0);
              if (characterization.getId() == null) {
                ReflectionTestUtils.setField(characterization, "id", 1000L);
              }
              return characterization;
            });
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), any()))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                Base64.getEncoder().encodeToString(bytes("back-image")),
                10));
    given(imageUploadService.uploadPublic(eq("characterization-result"), eq("image/png"), any()))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));
    givenBackUpload();

    service.characterize(1L, image, "솜구름");

    ArgumentCaptor<CardMetadata> metadataCaptor = ArgumentCaptor.forClass(CardMetadata.class);
    verify(characterizerClient)
        .characterize(any(), eq("image/png"), eq("솜구름"), metadataCaptor.capture());
    assertEquals("000", metadataCaptor.getValue().cardNo());
  }

  @Test
  void allowsAnimalNameUpToSixCharactersIncludingSpaces() {
    given(cardMetadataGenerator.generate()).willReturn(metadata());
    given(characterizerClient.characterize(any(), eq("image/png"), eq("가나다 라마"), any()))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                Base64.getEncoder().encodeToString(bytes("back-image")),
                10));
    given(imageUploadService.uploadPublic(eq("characterization-result"), eq("image/png"), any()))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));
    givenBackUpload();

    CharacterizationResponse response = service.characterize(1L, image(), " 가나다 라마 ");

    assertEquals("https://cdn.test/result.png", response.resultImageUrl());
  }

  @Test
  void rejectsAnimalNameLongerThanSixCharactersIncludingSpaces() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.characterize(1L, image(), "가나다 라마바"));

    assertEquals(ErrorCode.INVALID_ANIMAL_NAME.getCode(), exception.getCode());
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
    given(cardMetadataGenerator.generate()).willReturn(metadata());
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), any()))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                Base64.getEncoder().encodeToString(bytes("back-image")),
                10));
    given(imageUploadService.uploadPublic(eq("characterization-result"), eq("image/png"), any()))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));
    givenBackUpload();

    CharacterizationResponse response = service.characterize(1L, image(), "솜구름");

    assertEquals("https://cdn.test/result.png", response.resultImageUrl());
  }

  @Test
  void skipsDeviceLimitWhenDisabled() {
    CharacterizationService serviceWithLimitDisabled =
        new CharacterizationService(
            characterizationRepository,
            characterizerClient,
            cardMetadataGenerator,
            imageUploadService,
            new CharacterizationProperties(false));
    given(
            characterizationRepository.existsByDeviceIdAndStatus(
                1L, CharacterizationStatus.SUCCEEDED))
        .willReturn(true);
    given(cardMetadataGenerator.generate()).willReturn(metadata());
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), any()))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                "image/png",
                Base64.getEncoder().encodeToString(bytes("result-image")),
                Base64.getEncoder().encodeToString(bytes("back-image")),
                10));
    given(imageUploadService.uploadPublic(eq("characterization-result"), eq("image/png"), any()))
        .willReturn(new PublicUploadResponse("public/result.png", "https://cdn.test/result.png"));
    givenBackUpload();

    CharacterizationResponse response = serviceWithLimitDisabled.characterize(1L, image(), "솜구름");

    assertEquals("https://cdn.test/result.png", response.resultImageUrl());
  }

  @Test
  void recordsFailedWhenCharacterizerThrows() {
    given(cardMetadataGenerator.generate()).willReturn(metadata());
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), any()))
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
    given(cardMetadataGenerator.generate()).willReturn(metadata());
    given(characterizerClient.characterize(any(), eq("image/png"), eq("솜구름"), any()))
        .willReturn(
            new CharacterizerResult(
                "success",
                "codex_exec",
                "image/png",
                "not-base64",
                Base64.getEncoder().encodeToString(bytes("back-image")),
                10));

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

  private void givenBackUpload() {
    given(imageUploadService.uploadPublic(eq("characterization-back"), eq("image/png"), any()))
        .willReturn(new PublicUploadResponse("public/back.png", "https://cdn.test/back.png"));
  }

  private static CardMetadata metadata() {
    return metadata("001");
  }

  private static CardMetadata metadata(String cardNo) {
    return new CardMetadata(
        CardType.SKY, 82, CardSkill.SKY_CLOUD_JUMP, CardSkill.SKY_WIND_DASH, cardNo);
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
