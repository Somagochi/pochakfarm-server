package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.util.ReflectionTestUtils.setField;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnimalReadServiceTest {

  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final FileStorage fileStorage = mock(FileStorage.class);

  private final AnimalReadService service = new AnimalReadService(animalRepository, fileStorage);

  @Test
  void returnsEmptyWithoutQueryingWhenIdsAreEmpty() {
    assertTrue(service.getAnimals(List.of()).isEmpty());
    verifyNoInteractions(animalRepository, fileStorage);
  }

  @Test
  void buildsCardAndAnimalImageUrls() {
    given(animalRepository.findAllById(List.of(1L)))
        .willReturn(List.of(animal(1L, "솜구름", "card-key", "animal-key")));
    given(fileStorage.buildUrl("card-key")).willReturn("https://cdn.test/card.png");
    given(fileStorage.buildUrl("animal-key")).willReturn("https://cdn.test/animal.png");

    Map<Long, AnimalResponse> animals = service.getAnimals(List.of(1L));

    assertEquals("솜구름", animals.get(1L).animalName());
    assertEquals("https://cdn.test/card.png", animals.get(1L).cardImageUrl());
    assertEquals("https://cdn.test/animal.png", animals.get(1L).animalImageUrl());
  }

  @Test
  void returnsNullUrlWhenImageKeyIsMissing() {
    given(animalRepository.findAllById(List.of(1L)))
        .willReturn(List.of(animal(1L, "솜구름", null, null)));

    Map<Long, AnimalResponse> animals = service.getAnimals(List.of(1L));

    assertNull(animals.get(1L).cardImageUrl());
    assertNull(animals.get(1L).animalImageUrl());
    verifyNoInteractions(fileStorage);
  }

  private Animal animal(Long id, String animalName, String cardImageKey, String animalImageKey) {
    Animal animal = newInstance(Animal.class);
    setField(animal, "id", id);
    setField(animal, "animalName", animalName);
    setField(animal, "cardType", CardType.SEA);
    setField(animal, "cardImageKey", cardImageKey);
    setField(animal, "animalImageKey", animalImageKey);
    return animal;
  }

  private static <T> T newInstance(Class<T> type) {
    try {
      Constructor<T> constructor = type.getDeclaredConstructor();
      constructor.setAccessible(true);
      return constructor.newInstance();
    } catch (ReflectiveOperationException e) {
      throw new IllegalStateException(e);
    }
  }
}
