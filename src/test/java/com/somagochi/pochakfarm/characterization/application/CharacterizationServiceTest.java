package com.somagochi.pochakfarm.characterization.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardMetadataGenerator;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import com.somagochi.pochakfarm.characterization.dto.CharacterizationStartResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.properties.CharacterizationProperties;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class CharacterizationServiceTest {

  private final CharacterizationRepository characterizationRepository =
      mock(CharacterizationRepository.class);
  private final CardMetadataGenerator cardMetadataGenerator = mock(CardMetadataGenerator.class);
  private final CharacterizationAsyncService characterizationAsyncService =
      mock(CharacterizationAsyncService.class);
  private final CharacterizationService service =
      new CharacterizationService(
          characterizationRepository,
          cardMetadataGenerator,
          characterizationAsyncService,
          new CharacterizationProperties(true));

  @Test
  void startsCharacterizationAndReturnsIdStatusAndCardTypeWithoutWaitingForResult() {
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

    CharacterizationStartResponse response = service.characterize(1L, image, " 솜구름 ");

    assertEquals(1000L, response.characterizationId());
    assertEquals(CharacterizationStatus.PROCESSING, response.status());
    assertEquals(CardType.SKY, response.cardType());

    ArgumentCaptor<CardMetadata> metadataCaptor = ArgumentCaptor.forClass(CardMetadata.class);
    verify(characterizationAsyncService)
        .characterizeAsync(
            eq(1000L),
            eq(bytes("original-image")),
            eq("image/png"),
            eq("솜구름"),
            metadataCaptor.capture());
    assertEquals("000", metadataCaptor.getValue().cardNo());
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
  void skipsDeviceLimitWhenDisabled() {
    CharacterizationService serviceWithLimitDisabled =
        new CharacterizationService(
            characterizationRepository,
            cardMetadataGenerator,
            characterizationAsyncService,
            new CharacterizationProperties(false));
    given(
            characterizationRepository.existsByDeviceIdAndStatus(
                1L, CharacterizationStatus.SUCCEEDED))
        .willReturn(true);
    given(cardMetadataGenerator.generate()).willReturn(metadata());
    given(characterizationRepository.save(any(Characterization.class)))
        .willAnswer(
            invocation -> {
              Characterization characterization = invocation.getArgument(0);
              ReflectionTestUtils.setField(characterization, "id", 1L);
              return characterization;
            });

    CharacterizationStartResponse response =
        serviceWithLimitDisabled.characterize(1L, image(), "솜구름");

    assertEquals(1L, response.characterizationId());
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
