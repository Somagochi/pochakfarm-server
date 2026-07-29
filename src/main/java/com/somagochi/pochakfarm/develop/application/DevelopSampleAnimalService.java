package com.somagochi.pochakfarm.develop.application;

import com.somagochi.pochakfarm.animal.application.AnimalQueryService;
import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.dto.AnimalPosition;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import com.somagochi.pochakfarm.develop.dto.DevelopSampleAnimalResponse;
import com.somagochi.pochakfarm.farm.application.FarmQueryService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile({"local", "dev"})
@RequiredArgsConstructor
public class DevelopSampleAnimalService {

  private static final String SAMPLE_ORIGINAL_IMAGE_KEY = "develop/sample-original-image";
  private static final String SAMPLE_ORIGINAL_IMAGE_CONTENT_TYPE = "image/png";
  private static final Duration SAMPLE_GAME_RESULT_TTL = Duration.ofDays(365);

  private final FarmQueryService farmQueryService;
  private final AnimalQueryService animalQueryService;
  private final AnimalRepository animalRepository;
  private final CaptureRepository captureRepository;
  private final RandomProvider randomProvider;

  @Transactional
  public DevelopSampleAnimalResponse createSampleAnimal(Long userId) {
    int createdCount = 0;
    for (CardType cardType : CardType.values()) {
      FarmSpace space = farmQueryService.getSpace(userId, cardType);
      createdCount += placeSampleAnimals(userId, space);
    }
    return new DevelopSampleAnimalResponse(createdCount);
  }

  private int placeSampleAnimals(Long userId, FarmSpace space) {
    List<AnimalPosition> emptyPositions = findEmptyPositions(space);
    if (emptyPositions.isEmpty()) {
      return 0;
    }
    Collections.shuffle(emptyPositions);
    int count = randomProvider.nextInt(emptyPositions.size()) + 1;
    for (AnimalPosition position : emptyPositions.subList(0, count)) {
      Capture capture = captureRepository.save(sampleCapture(userId, space.getType()));
      animalRepository.save(
          Animal.create(capture.getId(), space.getId(), position.floorNum(), position.slotNum()));
    }
    return count;
  }

  private List<AnimalPosition> findEmptyPositions(FarmSpace space) {
    Set<AnimalPosition> occupied =
        animalQueryService
            .getByFloorRange(space.getId(), FarmSpace.FIRST_FLOOR, space.getFloor())
            .keySet();
    List<AnimalPosition> emptyPositions = new ArrayList<>();
    for (int floorNum = FarmSpace.FIRST_FLOOR; floorNum <= space.getFloor(); floorNum++) {
      for (int slotNum = FarmSpace.FIRST_SLOT;
          slotNum <= FarmSpace.SLOT_COUNT_PER_FLOOR;
          slotNum++) {
        AnimalPosition position = new AnimalPosition(floorNum, slotNum);
        if (!occupied.contains(position)) {
          emptyPositions.add(position);
        }
      }
    }
    return emptyPositions;
  }

  private Capture sampleCapture(Long userId, CardType cardType) {
    return Capture.create(
        userId,
        UUID.randomUUID().toString(),
        cardType,
        randomTier(),
        SAMPLE_ORIGINAL_IMAGE_KEY,
        SAMPLE_ORIGINAL_IMAGE_CONTENT_TYPE,
        Instant.now().plus(SAMPLE_GAME_RESULT_TTL));
  }

  private Tier randomTier() {
    Tier[] tiers = Tier.values();
    return tiers[randomProvider.nextInt(tiers.length)];
  }
}
