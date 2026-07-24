package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.List;
import java.util.Map;
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
