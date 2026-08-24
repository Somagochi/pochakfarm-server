package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AnimalRestService {

  private final AnimalRepository animalRepository;

  @Transactional(propagation = Propagation.MANDATORY)
  public void reserveRestByCaptureIds(
      Collection<Long> captureIds, Instant restEndsAt, Instant now) {
    List<Long> distinctCaptureIds = distinct(captureIds);
    List<Long> animalIds =
        animalRepository.findByCaptureIdIn(distinctCaptureIds).stream().map(Animal::getId).toList();
    if (animalIds.size() != distinctCaptureIds.size()) {
      throw new BusinessException(ErrorCode.ANIMAL_NOT_FOUND);
    }
    reserveRest(animalIds, restEndsAt, now);
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void reserveRest(Collection<Long> animalIds, Instant restEndsAt, Instant now) {
    for (Long animalId : distinct(animalIds).stream().sorted().toList()) {
      if (animalRepository.reserveRest(animalId, restEndsAt, now) != 1) {
        throw new BusinessException(ErrorCode.BATTLE_ANIMAL_RESTING);
      }
    }
  }

  private List<Long> distinct(Collection<Long> ids) {
    List<Long> distinctIds = ids.stream().distinct().toList();
    if (distinctIds.size() != ids.size()) {
      throw new BusinessException(ErrorCode.INVALID_BATTLE_ENTRY);
    }
    return distinctIds;
  }
}
