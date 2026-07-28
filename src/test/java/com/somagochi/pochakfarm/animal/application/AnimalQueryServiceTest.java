package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalDetailResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnimalQueryServiceTest {

  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final CaptureRepository captureRepository = mock(CaptureRepository.class);
  private final FileStorage fileStorage = mock(FileStorage.class);
  private final AnimalQueryService service =
      new AnimalQueryService(animalRepository, captureRepository, fileStorage);

  @Test
  void getBySlotIdsReturnsEmptyWithoutQueryingWhenEmpty() {
    assertTrue(service.getBySlotIds(List.of()).isEmpty());
    verifyNoInteractions(animalRepository, captureRepository, fileStorage);
  }

  @Test
  void getBySlotIdsMapsSlotIdToAnimal() {
    Animal animal = animal(7L, 100L);
    Capture capture = capture(7L);
    given(animalRepository.findBySlotIdIn(List.of(100L))).willReturn(List.of(animal));
    given(captureRepository.findAllById(any())).willReturn(List.of(capture));

    Map<Long, AnimalResponse> bySlotId = service.getBySlotIds(List.of(100L));

    assertEquals(1, bySlotId.size());
    assertEquals(7L, bySlotId.get(100L).animalId());
  }

  @Test
  void getAnimalReturnsDetailWithMappedSkills() {
    Capture capture = mock(Capture.class);
    given(capture.getAnimalName()).willReturn("바다냥");
    given(capture.getCardType()).willReturn(CardType.SEA);
    given(capture.getTier()).willReturn(Tier.A);
    given(capture.getSkill1()).willReturn(CardSkill.SEA_WAVE_DASH);
    given(capture.getSkill2()).willReturn(CardSkill.SEA_BUBBLE_GUARD);
    given(capture.getCardImage()).willReturn("card-key");
    given(capture.getAnimalImage()).willReturn("animal-key");
    given(fileStorage.buildUrl("card-key")).willReturn("https://cdn/card");
    given(fileStorage.buildUrl("animal-key")).willReturn("https://cdn/animal");
    given(captureRepository.findByUserIdAndAnimalId(1L, 10L)).willReturn(Optional.of(capture));

    AnimalDetailResponse response = service.getAnimal(1L, 10L);

    assertEquals(10L, response.animalId());
    assertEquals("바다냥", response.animalName());
    assertEquals(CardType.SEA, response.cardType());
    assertEquals(Tier.A, response.tier());
    assertEquals(CardSkill.SEA_WAVE_DASH, response.skill1());
    assertEquals(CardSkill.SEA_BUBBLE_GUARD, response.skill2());
    assertEquals("https://cdn/card", response.cardImageUrl());
    assertEquals("https://cdn/animal", response.animalImageUrl());
  }

  @Test
  void getAnimalKeepsNullSkillAsNull() {
    Capture capture = mock(Capture.class);
    given(capture.getCardType()).willReturn(CardType.SEA);
    given(capture.getTier()).willReturn(Tier.A);
    given(capture.getSkill1()).willReturn(null);
    given(capture.getSkill2()).willReturn(null);
    given(captureRepository.findByUserIdAndAnimalId(1L, 10L)).willReturn(Optional.of(capture));

    AnimalDetailResponse response = service.getAnimal(1L, 10L);

    assertNull(response.skill1());
    assertNull(response.skill2());
  }

  @Test
  void getAnimalThrowsWhenCaptureNotFound() {
    given(captureRepository.findByUserIdAndAnimalId(1L, 10L)).willReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service.getAnimal(1L, 10L));
  }

  private Animal animal(long id, Long slotId) {
    Animal animal = mock(Animal.class);
    given(animal.getId()).willReturn(id);
    given(animal.getCaptureId()).willReturn(id);
    given(animal.getSlotId()).willReturn(slotId);
    return animal;
  }

  private Capture capture(long id) {
    Capture capture = mock(Capture.class);
    given(capture.getId()).willReturn(id);
    given(capture.getAnimalName()).willReturn("동물" + id);
    given(capture.getCardType()).willReturn(CardType.SEA);
    given(capture.getTier()).willReturn(Tier.A);
    given(capture.getCardImage()).willReturn("card-" + id);
    given(capture.getAnimalImage()).willReturn("animal-" + id);
    return capture;
  }
}
