package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;

class AnimalQueryServiceTest {

  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final CaptureRepository captureRepository = mock(CaptureRepository.class);
  private final FileStorage fileStorage = mock(FileStorage.class);
  private final AnimalQueryService service =
      new AnimalQueryService(animalRepository, captureRepository, fileStorage);

  @Test
  void getMyAnimalsReturnsFirstPageWithNextCursorWhenMoreExist() {
    List<Animal> animals = new ArrayList<>();
    List<Capture> captures = new ArrayList<>();
    for (int i = 0; i < 13; i++) {
      long id = 100L - i;
      animals.add(animal(id, null));
      captures.add(capture(id));
    }
    given(captureRepository.findByUserId(1L)).willReturn(captures);
    given(
            animalRepository.findByCaptureIdInAndIdLessThanOrderByIdDesc(
                any(), eq(Long.MAX_VALUE), eq(PageRequest.of(0, 13))))
        .willReturn(animals);

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, null, null);

    assertEquals(12, page.content().size());
    assertEquals(100L, page.content().get(0).animalId());
    assertTrue(page.hasNext());
    assertEquals(89L, page.nextCursor());
  }

  @Test
  void getMyAnimalsHasNoNextOnLastPage() {
    Capture capture = capture(50L);
    Animal animal = animal(50L, null);
    given(captureRepository.findByUserId(1L)).willReturn(List.of(capture));
    given(
            animalRepository.findByCaptureIdInAndIdLessThanOrderByIdDesc(
                any(), eq(Long.MAX_VALUE), eq(PageRequest.of(0, 13))))
        .willReturn(List.of(animal));
    given(fileStorage.buildUrl("card-50")).willReturn("https://cdn.test/card.png");

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, null, null);

    assertEquals(1, page.content().size());
    assertEquals(50L, page.content().get(0).animalId());
    assertEquals("https://cdn.test/card.png", page.content().get(0).cardImageUrl());
    assertFalse(page.hasNext());
    assertNull(page.nextCursor());
  }

  @Test
  void getMyAnimalsUsesCursorAndCardType() {
    Capture capture = capture(30L);
    Animal animal = animal(30L, null);
    given(captureRepository.findByUserIdAndCardType(1L, CardType.SEA)).willReturn(List.of(capture));
    given(
            animalRepository.findByCaptureIdInAndIdLessThanOrderByIdDesc(
                any(), eq(50L), eq(PageRequest.of(0, 13))))
        .willReturn(List.of(animal));

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, CardType.SEA, 50L);

    assertEquals(1, page.content().size());
    assertEquals(30L, page.content().get(0).animalId());
    assertEquals(CardType.SEA, page.content().get(0).cardType());
    assertFalse(page.hasNext());
  }

  @Test
  void getMyAnimalsReturnsEmptyWhenUserHasNoCaptures() {
    given(captureRepository.findByUserId(1L)).willReturn(List.of());

    CursorPage<AnimalResponse> page = service.getMyAnimals(1L, null, null);

    assertTrue(page.content().isEmpty());
    assertFalse(page.hasNext());
    assertNull(page.nextCursor());
    verifyNoInteractions(animalRepository);
  }

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
