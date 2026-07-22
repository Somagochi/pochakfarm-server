package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalReadService {

  private final AnimalRepository animalRepository;
  private final FileStorage fileStorage;

  public AnimalReadService(AnimalRepository animalRepository, FileStorage fileStorage) {
    this.animalRepository = animalRepository;
    this.fileStorage = fileStorage;
  }

  @Transactional(readOnly = true)
  public Map<Long, AnimalResponse> getAnimals(Collection<Long> animalIds) {
    if (animalIds.isEmpty()) {
      return Map.of();
    }
    return animalRepository.findAllById(animalIds).stream()
        .collect(Collectors.toMap(Animal::getId, this::toResponse));
  }

  private AnimalResponse toResponse(Animal animal) {
    return new AnimalResponse(
        animal.getId(),
        animal.getAnimalName(),
        animal.getStatus(),
        buildUrlOrNull(animal.getCardImageKey()),
        buildUrlOrNull(animal.getAnimalImageKey()),
        animal.getCardType(),
        animal.getFailureReason());
  }

  private String buildUrlOrNull(String key) {
    return key == null ? null : fileStorage.buildUrl(key);
  }
}
