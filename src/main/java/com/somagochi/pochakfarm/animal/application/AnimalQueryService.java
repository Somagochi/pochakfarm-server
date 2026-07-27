package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalDetailResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalQueryService {

  private final AnimalRepository animalRepository;
  private final CaptureRepository captureRepository;
  private final FileStorage fileStorage;

  public AnimalQueryService(
      AnimalRepository animalRepository,
      CaptureRepository captureRepository,
      FileStorage fileStorage) {
    this.animalRepository = animalRepository;
    this.captureRepository = captureRepository;
    this.fileStorage = fileStorage;
  }

  @Transactional(readOnly = true)
  public AnimalDetailResponse getAnimal(Long userId, Long animalId) {
    Capture capture =
        captureRepository
            .findByUserIdAndAnimalId(userId, animalId)
            .orElseThrow(() -> new BusinessException(ErrorCode.ANIMAL_NOT_FOUND));
    return new AnimalDetailResponse(
        animalId,
        capture.getAnimalName(),
        capture.getCardType(),
        capture.getTier(),
        toCardSkill(capture.getSkill1()),
        toCardSkill(capture.getSkill2()),
        buildUrlOrNull(capture.getCardImage()),
        buildUrlOrNull(capture.getAnimalImage()));
  }

  private CardSkill toCardSkill(Integer skillOrdinal) {
    return skillOrdinal == null ? null : CardSkill.values()[skillOrdinal];
  }

  @Transactional(readOnly = true)
  public Map<Long, AnimalResponse> getBySlotIds(Collection<Long> slotIds) {
    if (slotIds.isEmpty()) {
      return Map.of();
    }
    List<Animal> animals = animalRepository.findBySlotIdIn(slotIds);
    Map<Long, Capture> captureById = findCapturesById(animals);
    Map<Long, AnimalResponse> bySlotId = new HashMap<>();
    for (Animal animal : animals) {
      Capture capture = captureById.get(animal.getCaptureId());
      if (capture != null) {
        bySlotId.put(animal.getSlotId(), toResponse(animal, capture));
      }
    }
    return bySlotId;
  }

  private Map<Long, Capture> findCapturesById(List<Animal> animals) {
    Set<Long> captureIds = animals.stream().map(Animal::getCaptureId).collect(Collectors.toSet());
    return captureRepository.findAllById(captureIds).stream()
        .collect(Collectors.toMap(Capture::getId, capture -> capture));
  }

  private AnimalResponse toResponse(Animal animal, Capture capture) {
    return new AnimalResponse(
        animal.getId(),
        capture.getAnimalName(),
        capture.getCardType(),
        capture.getTier(),
        buildUrlOrNull(capture.getCardImage()),
        buildUrlOrNull(capture.getAnimalImage()));
  }

  private String buildUrlOrNull(String key) {
    return key == null ? null : fileStorage.buildUrl(key);
  }
}
