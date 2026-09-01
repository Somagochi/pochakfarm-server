package com.somagochi.pochakfarm.battle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.dto.BattleEntryRequest;
import com.somagochi.pochakfarm.battle.dto.BattleStartRequest;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class BattleStartConcurrencyTest {

  private static final Instant NOW = Instant.parse("2026-08-25T05:00:00Z");
  private static final int THREAD_COUNT = 4;

  @Autowired private BattleStartService battleStartService;
  @Autowired private BattleRepository battleRepository;
  @Autowired private BattleFixtures fixtures;

  private Long userId;
  private GymLeader gymLeader;
  private List<Animal> myAnimals;

  @BeforeEach
  void setUp() {
    fixtures.cleanUp();
    userId = fixtures.createUser();
    gymLeader = fixtures.createGymLeader(1, BattlePolicy.ENTRY_COUNT);
    myAnimals =
        List.of(
            fixtures.createAnimal(userId, CardType.SKY, Tier.A, CardSkill.SKY_FEATHER_GUARD),
            fixtures.createAnimal(userId, CardType.SKY, Tier.B, CardSkill.SKY_TAILWIND),
            fixtures.createAnimal(userId, CardType.SKY, Tier.C, CardSkill.SKY_CLOUD_CUSHION));
  }

  @AfterEach
  void tearDown() {
    fixtures.cleanUp();
  }

  @Test
  void createsOnlyOneBattleWhenSameAnimalsAreSubmittedConcurrently() throws Exception {
    CountDownLatch ready = new CountDownLatch(THREAD_COUNT);
    CountDownLatch start = new CountDownLatch(1);
    CountDownLatch done = new CountDownLatch(THREAD_COUNT);
    AtomicInteger succeeded = new AtomicInteger();
    AtomicInteger rejected = new AtomicInteger();

    try (ExecutorService executor = Executors.newFixedThreadPool(THREAD_COUNT)) {
      IntStream.range(0, THREAD_COUNT)
          .forEach(
              index ->
                  executor.submit(
                      () -> {
                        ready.countDown();
                        try {
                          start.await();
                          battleStartService.start(userId, request(), NOW);
                          succeeded.incrementAndGet();
                        } catch (Exception rejection) {
                          rejected.incrementAndGet();
                        } finally {
                          done.countDown();
                        }
                      }));

      ready.await(5, TimeUnit.SECONDS);
      start.countDown();
      done.await(20, TimeUnit.SECONDS);
    }

    assertEquals(1, succeeded.get());
    assertEquals(THREAD_COUNT - 1, rejected.get());
    assertEquals(1, battleRepository.findAll().size());
  }

  private BattleStartRequest request() {
    List<BattleEntryRequest> entries =
        IntStream.range(0, myAnimals.size())
            .mapToObj(index -> new BattleEntryRequest(myAnimals.get(index).getId(), index + 1))
            .toList();
    return new BattleStartRequest(gymLeader.getId(), UUID.randomUUID().toString(), entries);
  }
}
