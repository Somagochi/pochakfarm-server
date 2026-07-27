package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class AnimalDeleteServiceTest {

  private final AnimalRepository animalRepository = mock(AnimalRepository.class);
  private final AnimalDeleteService service = new AnimalDeleteService(animalRepository);

  @Test
  void deleteAnimalRemovesOwnedAnimal() {
    Animal animal = mock(Animal.class);
    given(animalRepository.findOwnedAnimal(1L, 10L)).willReturn(Optional.of(animal));

    service.deleteAnimal(1L, 10L);

    verify(animal).remove();
  }

  @Test
  void deleteAnimalThrowsWhenNotOwnedOrMissing() {
    given(animalRepository.findOwnedAnimal(1L, 10L)).willReturn(Optional.empty());

    assertThrows(BusinessException.class, () -> service.deleteAnimal(1L, 10L));
  }
}
