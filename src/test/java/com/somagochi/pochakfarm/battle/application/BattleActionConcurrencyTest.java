package com.somagochi.pochakfarm.battle.application;

import static com.somagochi.pochakfarm.battle.application.BattleFixture.NPC_GAMBLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.NPC_STABLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.USER_GAMBLE_SKILL;
import static com.somagochi.pochakfarm.battle.application.BattleFixture.USER_STABLE_SKILL;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.battle.dto.BattleActionRequest;
import com.somagochi.pochakfarm.battle.dto.BattleActionResponse;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleActionRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleBroadcastEventRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.common.random.RandomProvider;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class BattleActionConcurrencyTest {

  private static final Long USER_ID = 21200L;
  private static final Instant STARTED_AT = Instant.parse("2026-08-25T00:00:00Z");
  private static final int PERCENTAGE_BOUND = 100;

  @Autowired private BattleActionService battleActionService;
  @Autowired private BattleRepository battleRepository;
  @Autowired private BattleEntryRepository battleEntryRepository;
  @Autowired private BattleActionRepository battleActionRepository;
  @Autowired private BattleBroadcastEventRepository battleBroadcastEventRepository;
  @Autowired private GymLeaderRepository gymLeaderRepository;
  @Autowired private GymLeaderAnimalRepository gymLeaderAnimalRepository;

  @MockitoBean private RandomProvider randomProvider;
  @MockitoBean private Clock clock;

  private final ExecutorService executor = Executors.newFixedThreadPool(2);

  @BeforeEach
  void setUp() {
    battleActionRepository.deleteAll();
    battleBroadcastEventRepository.deleteAll();
    battleEntryRepository.deleteAll();
    battleRepository.deleteAll();
    gymLeaderAnimalRepository.deleteAll();
    gymLeaderRepository.deleteAll();
    given(clock.instant()).willReturn(STARTED_AT);
    given(randomProvider.nextInt(PERCENTAGE_BOUND)).willReturn(0);
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void appliesSameActionSeqOnlyOnceWhenRequestedConcurrently() throws Exception {
    Long battleId = startBattle();

    List<Optional<BattleActionResponse>> outcomes =
        runConcurrently(select(battleId), select(battleId));

    List<BattleActionResponse> succeeded = outcomes.stream().flatMap(Optional::stream).toList();
    assertFalse(succeeded.isEmpty());
    assertEquals(1, battleActionRepository.countByBattleId(battleId));
    assertEquals(1, battleActionRepository.findByBattleIdOrderByActionSeqAsc(battleId).size());
    assertTrue(
        succeeded.stream()
            .allMatch(
                response ->
                    response.netPoint() == succeeded.getFirst().netPoint()
                        && response.barPosition() == succeeded.getFirst().barPosition()
                        && response.user().skill() == succeeded.getFirst().user().skill()
                        && response.npc().skill() == succeeded.getFirst().npc().skill()));
    assertEquals(
        succeeded.getFirst().barPosition(),
        battleRepository.findById(battleId).orElseThrow().getBarPosition());
  }

  private Callable<Optional<BattleActionResponse>> select(Long battleId) {
    return () -> {
      try {
        return Optional.of(
            battleActionService.selectSkill(
                USER_ID, battleId, new BattleActionRequest(1, USER_STABLE_SKILL)));
      } catch (RuntimeException e) {
        return Optional.empty();
      }
    };
  }

  private List<Optional<BattleActionResponse>> runConcurrently(
      Callable<Optional<BattleActionResponse>> first,
      Callable<Optional<BattleActionResponse>> second)
      throws Exception {
    CountDownLatch startLatch = new CountDownLatch(1);
    List<Future<Optional<BattleActionResponse>>> futures = new ArrayList<>();
    for (Callable<Optional<BattleActionResponse>> task : List.of(first, second)) {
      futures.add(
          executor.submit(
              () -> {
                startLatch.await(5, TimeUnit.SECONDS);
                return task.call();
              }));
    }
    startLatch.countDown();

    List<Optional<BattleActionResponse>> outcomes = new ArrayList<>();
    for (Future<Optional<BattleActionResponse>> future : futures) {
      outcomes.add(future.get(20, TimeUnit.SECONDS));
    }
    return outcomes;
  }

  private Long startBattle() {
    return new BattleFixture(
            battleRepository, battleEntryRepository, gymLeaderRepository, gymLeaderAnimalRepository)
        .userSkills(USER_STABLE_SKILL, USER_GAMBLE_SKILL)
        .npcSkills(NPC_STABLE_SKILL, NPC_GAMBLE_SKILL)
        .start(USER_ID, STARTED_AT)
        .getId();
  }
}
