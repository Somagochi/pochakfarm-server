package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class AnimalRestConcurrencyTest {

  private static final String SUCCESS = "SUCCESS";
  private static final String CONFLICT = "CONFLICT";
  private static final Instant NOW = Instant.parse("2026-08-19T01:00:00Z");
  private static final Duration REST_DURATION = Duration.ofMinutes(30);

  @Autowired private AnimalRestService animalRestService;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private FarmSpaceRepository farmSpaceRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PlatformTransactionManager transactionManager;

  private final ExecutorService executor = Executors.newFixedThreadPool(2);
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
                "rest-concurrency-" + UUID.randomUUID(),
                "rest-concurrency@test.com",
                "u" + UUID.randomUUID().toString().substring(0, 5)));
    userId = user.getId();
    spaceId = farmSpaceRepository.save(FarmSpace.create(userId, CardType.SEA)).getId();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void reservesSameAnimalOnlyOnceWhenTwoBattlesStartTogether() throws Exception {
    Long animalId = placeAnimal(1);

    List<String> outcomes = runConcurrently(reserve(List.of(animalId)), reserve(List.of(animalId)));

    assertEquals(1, outcomes.stream().filter(SUCCESS::equals).count());
    assertTrue(outcomes.contains(ErrorCode.BATTLE_ANIMAL_RESTING.getCode()));
    assertNotNull(animalRepository.findById(animalId).orElseThrow().getRestEndsAt());
  }

  @Test
  void rollsBackEveryReservationWhenOneAnimalIsAlreadyTaken() throws Exception {
    Long sharedAnimalId = placeAnimal(1);
    Long firstOnlyAnimalId = placeAnimal(2);
    Long secondOnlyAnimalId = placeAnimal(3);

    List<String> outcomes =
        runConcurrently(
            reserve(List.of(sharedAnimalId, firstOnlyAnimalId)),
            reserve(List.of(sharedAnimalId, secondOnlyAnimalId)));

    assertEquals(1, outcomes.stream().filter(SUCCESS::equals).count());
    assertNotNull(animalRepository.findById(sharedAnimalId).orElseThrow().getRestEndsAt());
    long reserved =
        List.of(firstOnlyAnimalId, secondOnlyAnimalId).stream()
            .map(id -> animalRepository.findById(id).orElseThrow())
            .filter(animal -> animal.getRestEndsAt() != null)
            .count();
    assertEquals(1, reserved);
  }

  @Test
  void reservesDisjointAnimalsConcurrently() throws Exception {
    Long firstAnimalId = placeAnimal(1);
    Long secondAnimalId = placeAnimal(2);

    List<String> outcomes =
        runConcurrently(reserve(List.of(firstAnimalId)), reserve(List.of(secondAnimalId)));

    assertEquals(2, outcomes.stream().filter(SUCCESS::equals).count());
    assertNotNull(animalRepository.findById(firstAnimalId).orElseThrow().getRestEndsAt());
    assertNotNull(animalRepository.findById(secondAnimalId).orElseThrow().getRestEndsAt());
  }

  @Test
  void keepsRestUnsetWhenTheBattleTransactionFails() {
    Long animalId = placeAnimal(1);

    assertThrows(
        IllegalStateException.class,
        () ->
            transactionTemplate.executeWithoutResult(
                status -> {
                  animalRestService.reserveRest(List.of(animalId), NOW.plus(REST_DURATION), NOW);
                  throw new IllegalStateException("battle creation failed");
                }));

    assertNull(animalRepository.findById(animalId).orElseThrow().getRestEndsAt());
  }

  private Callable<String> reserve(List<Long> animalIds) {
    return () -> {
      try {
        transactionTemplate.executeWithoutResult(
            status -> animalRestService.reserveRest(animalIds, NOW.plus(REST_DURATION), NOW));
        return SUCCESS;
      } catch (OptimisticLockingFailureException | PessimisticLockingFailureException exception) {
        return CONFLICT;
      } catch (BusinessException exception) {
        return exception.getCode();
      }
    };
  }

  private List<String> runConcurrently(Callable<String> first, Callable<String> second)
      throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<String>> futures = new ArrayList<>();
    for (Callable<String> request : List.of(first, second)) {
      futures.add(
          executor.submit(
              () -> {
                ready.countDown();
                start.await(5, TimeUnit.SECONDS);
                return request.call();
              }));
    }
    ready.await(5, TimeUnit.SECONDS);
    start.countDown();
    List<String> outcomes = new ArrayList<>();
    for (Future<String> future : futures) {
      outcomes.add(future.get(10, TimeUnit.SECONDS));
    }
    return outcomes;
  }

  private Long placeAnimal(int slotNum) {
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
    return animalRepository.save(Animal.create(capture.getId(), spaceId, 1, slotNum)).getId();
  }
}
