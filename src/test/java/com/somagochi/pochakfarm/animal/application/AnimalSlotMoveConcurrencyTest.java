package com.somagochi.pochakfarm.animal.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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

@SpringBootTest
class AnimalSlotMoveConcurrencyTest {

  private static final String SUCCESS = "SUCCESS";
  private static final String CONFLICT = "CONFLICT";

  @Autowired private AnimalSlotMoveService animalSlotMoveService;
  @Autowired private AnimalRepository animalRepository;
  @Autowired private FarmSpaceRepository farmSpaceRepository;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private UserRepository userRepository;

  private final ExecutorService executor = Executors.newFixedThreadPool(2);
  private Long userId;
  private Long spaceId;

  @BeforeEach
  void setUp() {
    animalRepository.deleteAll();
    farmSpaceRepository.deleteAll();
    captureRepository.deleteAll();
    userRepository.deleteAll();
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                "move-" + UUID.randomUUID(),
                "move@test.com",
                "u" + UUID.randomUUID().toString().substring(0, 5)));
    userId = user.getId();
    spaceId = farmSpaceRepository.save(FarmSpace.create(userId, CardType.SEA)).getId();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void movesSameAnimalConcurrentlyWithoutLosingPosition() throws Exception {
    Long animalId = placeAnimal(1, 1);

    List<String> outcomes = runConcurrently(move(animalId, 1, 2), move(animalId, 1, 3));

    assertTrue(outcomes.stream().allMatch(o -> SUCCESS.equals(o) || CONFLICT.equals(o)));
    assertTrue(outcomes.contains(SUCCESS));
    Animal animal = animalRepository.findById(animalId).orElseThrow();
    assertTrue(animal.getSlotNum() == 2 || animal.getSlotNum() == 3);
    assertNoDuplicateOccupancy();
  }

  @Test
  void keepsSingleOccupantWhenTwoAnimalsMoveIntoSameEmptySlot() throws Exception {
    Long firstAnimalId = placeAnimal(1, 1);
    Long secondAnimalId = placeAnimal(1, 2);

    List<String> outcomes = runConcurrently(move(firstAnimalId, 1, 3), move(secondAnimalId, 1, 3));

    assertTrue(outcomes.stream().allMatch(o -> SUCCESS.equals(o) || CONFLICT.equals(o)));
    assertTrue(outcomes.contains(SUCCESS));
    assertEquals(
        1,
        animalRepository.findAll().stream().filter(animal -> animal.isAt(spaceId, 1, 3)).count());
    assertNoDuplicateOccupancy();
  }

  @Test
  void keepsConsistentPositionsWhenSwapTargetMovesConcurrently() throws Exception {
    Long firstAnimalId = placeAnimal(1, 1);
    Long secondAnimalId = placeAnimal(1, 2);

    List<String> outcomes = runConcurrently(move(firstAnimalId, 1, 2), move(secondAnimalId, 1, 4));

    assertTrue(outcomes.stream().allMatch(o -> SUCCESS.equals(o) || CONFLICT.equals(o)));
    assertTrue(outcomes.contains(SUCCESS));
    assertNoDuplicateOccupancy();
  }

  private Callable<String> move(Long animalId, int floorNum, int slotNum) {
    return () -> {
      try {
        animalSlotMoveService.moveToSlot(userId, animalId, floorNum, slotNum);
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

  private Long placeAnimal(int floorNum, int slotNum) {
    Capture capture =
        captureRepository.save(
            Capture.create(
                userId,
                UUID.randomUUID().toString(),
                CardType.SEA,
                Tier.C,
                AnimalName.from("이동동물"),
                CardSkill.SEA_WAVE_DASH,
                CardSkill.SEA_BUBBLE_GUARD,
                "001",
                "images/capture-original/%d/%s.jpg".formatted(userId, UUID.randomUUID()),
                "image/jpeg",
                Instant.now().plusSeconds(300)));
    return animalRepository
        .save(Animal.create(capture.getId(), spaceId, floorNum, slotNum))
        .getId();
  }

  private void assertNoDuplicateOccupancy() {
    Set<String> positions = new HashSet<>();
    for (Animal animal : animalRepository.findAll()) {
      assertTrue(
          positions.add(
              animal.getSpaceId() + ":" + animal.getFloorNum() + ":" + animal.getSlotNum()));
    }
  }
}
