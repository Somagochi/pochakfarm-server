package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class AnimalRestServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-19T01:00:00Z");
  private static final Duration REST_DURATION = Duration.ofMinutes(30);

  @Autowired private AnimalRestService animalRestService;
  @Autowired private AnimalQueryService animalQueryService;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private FarmSpaceRepository farmSpaceRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;
  private Long userId;
  private Long spaceId;

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
    animalRepository.deleteAll();
    farmSpaceRepository.deleteAll();
    captureRepository.deleteAll();
    userRepository.deleteAll();
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                "rest-" + UUID.randomUUID(),
                "rest@test.com",
                "u" + UUID.randomUUID().toString().substring(0, 5)));
    userId = user.getId();
    spaceId = farmSpaceRepository.save(FarmSpace.create(userId, CardType.SEA)).getId();
  }

  @Test
  void savesAndReadsRestEndsAtByCaptureId() {
    Animal animal = placeAnimal();
    Long captureId = animal.getCaptureId();

    reserveRestByCaptureIds(List.of(captureId));

    Map<Long, Instant> restEndsAt =
        animalQueryService.getRestEndsAtByCaptureIds(List.of(captureId));

    assertEquals(Map.of(captureId, NOW.plus(REST_DURATION)), restEndsAt);
    assertEquals(
        Set.of(captureId), animalQueryService.findRestingCaptureIds(List.of(captureId), NOW));
    assertTrue(
        animalQueryService
            .findRestingCaptureIds(List.of(captureId), NOW.plus(REST_DURATION))
            .isEmpty());
  }

  @Test
  void reportsNoRestBeforeAnyBattle() {
    Animal animal = placeAnimal();

    assertTrue(
        animalQueryService.getRestEndsAtByCaptureIds(List.of(animal.getCaptureId())).isEmpty());
    assertNull(animalRepository.findById(animal.getId()).orElseThrow().getRestEndsAt());
    assertFalse(animalRepository.findById(animal.getId()).orElseThrow().isResting(NOW));
  }

  @Test
  void rejectsAlreadyRestingAnimal() {
    Animal animal = placeAnimal();
    reserveRest(List.of(animal.getId()), NOW);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> reserveRest(List.of(animal.getId()), NOW.plusSeconds(60)));

    assertEquals(ErrorCode.BATTLE_ANIMAL_RESTING.getCode(), exception.getCode());
  }

  @Test
  void allowsReuseAfterRestEnded() {
    Animal animal = placeAnimal();
    reserveRest(List.of(animal.getId()), NOW);

    Instant later = NOW.plus(REST_DURATION);
    reserveRest(List.of(animal.getId()), later);

    assertEquals(
        later.plus(REST_DURATION),
        animalRepository.findById(animal.getId()).orElseThrow().getRestEndsAt());
  }

  @Test
  void bumpsVersionSoStaleEntitiesCannotOverwriteRest() {
    Animal animal = placeAnimal();
    long versionBefore = animalRepository.findById(animal.getId()).orElseThrow().getVersion();

    reserveRest(List.of(animal.getId()), NOW);

    assertEquals(
        versionBefore + 1, animalRepository.findById(animal.getId()).orElseThrow().getVersion());
  }

  @Test
  void rejectsDuplicateAnimalIdsInOneBattle() {
    Animal animal = placeAnimal();

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> reserveRest(List.of(animal.getId(), animal.getId()), NOW));

    assertEquals(ErrorCode.INVALID_BATTLE_ENTRY.getCode(), exception.getCode());
  }

  @Test
  void rejectsCaptureWithoutAnimal() {
    Long unknownCaptureId = -1L;

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> reserveRestByCaptureIds(List.of(unknownCaptureId)));

    assertEquals(ErrorCode.ANIMAL_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void requiresCallerTransaction() {
    Animal animal = placeAnimal();

    assertThrows(
        IllegalTransactionStateException.class,
        () -> animalRestService.reserveRest(List.of(animal.getId()), NOW.plus(REST_DURATION), NOW));
  }

  private void reserveRest(List<Long> animalIds, Instant now) {
    transactionTemplate.executeWithoutResult(
        status -> animalRestService.reserveRest(animalIds, now.plus(REST_DURATION), now));
  }

  private void reserveRestByCaptureIds(List<Long> captureIds) {
    transactionTemplate.executeWithoutResult(
        status ->
            animalRestService.reserveRestByCaptureIds(captureIds, NOW.plus(REST_DURATION), NOW));
  }

  private Animal placeAnimal() {
    Capture capture =
        captureRepository.save(
            Capture.create(
                userId,
                UUID.randomUUID().toString(),
                CardType.SEA,
                Tier.C,
                AnimalName.from("휴식동물"),
                CardSkill.SEA_WAVE_DASH,
                CardSkill.SEA_BUBBLE_GUARD,
                "001",
                "images/capture-original/%d/%s.jpg".formatted(userId, UUID.randomUUID()),
                "image/jpeg",
                NOW.plusSeconds(300)));
    return animalRepository.save(Animal.create(capture.getId(), spaceId, 1, 1));
  }
}
