package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalDeleteService {

  private final AnimalRepository animalRepository;

  public AnimalDeleteService(AnimalRepository animalRepository) {
    this.animalRepository = animalRepository;
  }

  @Transactional
  public void deleteAnimal(Long userId, Long animalId) {
    Animal animal =
        animalRepository
            .findOwnedAnimal(userId, animalId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    animalRepository.delete(animal);
  }
}
