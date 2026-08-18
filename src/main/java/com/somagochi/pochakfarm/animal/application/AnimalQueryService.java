package com.somagochi.pochakfarm.animal.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalDetailResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalPlacement;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.dto.AnimalResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalSkillResponse;
import com.somagochi.pochakfarm.animal.dto.AnimalTypeCount;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.response.CursorPage;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AnimalQueryService {

  private static final int PAGE_SIZE = 12;
  private static final char LIKE_ESCAPE_CHAR = '!';

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
  public CursorPage<AnimalResponse> getMyAnimals(Long userId, CardType type, Long cursor) {
    return toCursorPage(
        animalRepository.findOwnedAnimals(
            userId, cardTypesOf(type), effectiveCursor(cursor), Limit.of(PAGE_SIZE + 1)));
  }

  @Transactional(readOnly = true)
  public CursorPage<AnimalResponse> searchMyAnimals(
      Long userId, CardType type, String keyword, Long cursor) {
    return toCursorPage(
        animalRepository.searchOwnedAnimalsByName(
            userId,
            cardTypesOf(type),
            prefixPattern(normalizeKeyword(keyword)),
            effectiveCursor(cursor),
            Limit.of(PAGE_SIZE + 1)));
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
        AnimalSkillResponse.from(capture.getSkill1()),
        AnimalSkillResponse.from(capture.getSkill2()),
        buildUrlOrNull(capture.getCardImage()),
        buildUrlOrNull(capture.getAnimalImage()));
  }

  @Transactional(readOnly = true)
  public Map<CardType, Long> countOwnedByCardType(Long userId) {
    return animalRepository.countOwnedByCardType(userId).stream()
        .collect(Collectors.toMap(AnimalTypeCount::cardType, AnimalTypeCount::count));
  }

  @Transactional(readOnly = true)
  public List<AnimalPlacement> getPlacements(Long userId) {
    return animalRepository.findPlacedAnimals(userId).stream()
        .map(
            animal ->
                new AnimalPlacement(animal.getSpaceId(), animal.getFloorNum(), animal.getSlotNum()))
        .toList();
  }

  @Transactional(readOnly = true)
  public Map<AnimalPosition, AnimalResponse> getByFloorRange(
      Long spaceId, int firstFloorNum, int lastFloorNum) {
    List<Animal> animals =
        animalRepository.findBySpaceIdAndFloorNumBetween(spaceId, firstFloorNum, lastFloorNum);
    if (animals.isEmpty()) {
      return Map.of();
    }
    Map<Long, Capture> captureById = findCapturesById(animals);
    Map<AnimalPosition, AnimalResponse> byPosition = new HashMap<>();
    for (Animal animal : animals) {
      Capture capture = captureById.get(animal.getCaptureId());
      if (capture != null) {
        byPosition.put(
            new AnimalPosition(animal.getFloorNum(), animal.getSlotNum()),
            toResponse(animal, capture));
      }
    }
    return byPosition;
  }

  private CursorPage<AnimalResponse> toCursorPage(List<Animal> fetched) {
    if (fetched.isEmpty()) {
      return CursorPage.of(List.of(), null, false);
    }
    boolean hasNext = fetched.size() > PAGE_SIZE;
    List<Animal> page = hasNext ? fetched.subList(0, PAGE_SIZE) : fetched;
    Map<Long, Capture> captureById = findCapturesById(page);
    List<AnimalResponse> items =
        page.stream()
            .filter(animal -> captureById.containsKey(animal.getCaptureId()))
            .map(animal -> toResponse(animal, captureById.get(animal.getCaptureId())))
            .toList();
    Long nextCursor = hasNext ? page.get(page.size() - 1).getId() : null;
    return CursorPage.of(items, nextCursor, hasNext);
  }

  private String normalizeKeyword(String keyword) {
    if (keyword == null || keyword.isBlank()) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    return keyword.trim();
  }

  private String prefixPattern(String keyword) {
    StringBuilder pattern = new StringBuilder();
    for (char each : keyword.toCharArray()) {
      if (each == LIKE_ESCAPE_CHAR || each == '%' || each == '_') {
        pattern.append(LIKE_ESCAPE_CHAR);
      }
      pattern.append(each);
    }
    return pattern.append('%').toString();
  }

  private Collection<CardType> cardTypesOf(CardType type) {
    return type == null ? List.of(CardType.values()) : List.of(type);
  }

  private Map<Long, Capture> findCapturesById(List<Animal> animals) {
    Set<Long> captureIds = animals.stream().map(Animal::getCaptureId).collect(Collectors.toSet());
    return captureRepository.findAllById(captureIds).stream()
        .collect(Collectors.toMap(Capture::getId, capture -> capture));
  }

  private long effectiveCursor(Long cursor) {
    return cursor == null ? Long.MAX_VALUE : cursor;
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
