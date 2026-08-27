package com.somagochi.pochakfarm.battle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.dto.GymLeaderDetailResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderResponse;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class GymLeaderQueryServiceTest {

  @Autowired private GymLeaderQueryService gymLeaderQueryService;
  @Autowired private BattleFixtures fixtures;

  private Long userId;
  private GymLeader first;
  private GymLeader second;

  @BeforeEach
  void setUp() {
    fixtures.cleanUp();
    userId = fixtures.createUser();
    first = fixtures.createGymLeader(1, BattlePolicy.ENTRY_COUNT);
    second = fixtures.createGymLeader(2, BattlePolicy.ENTRY_COUNT);
  }

  @AfterEach
  void tearDown() {
    fixtures.cleanUp();
  }

  @Test
  void unlocksFirstGymLeaderWithoutAnyCondition() {
    GymLeaderResponse response = gymLeaderQueryService.getGymLeaders(userId).get(0);

    assertTrue(response.unlock().unlocked());
    assertEquals(1, response.unlock().requiredLevel());
    assertTrue(response.unlock().levelSatisfied());
    assertNull(response.unlock().previousBadgeCode());
    assertTrue(response.unlock().previousBadgeSatisfied());
  }

  @Test
  void reportsMissingPreviousBadgeSeparatelyFromLevel() {
    fixtures.changeLevel(userId, 40);

    GymLeaderResponse response = gymLeaderResponseOf(second);

    assertFalse(response.unlock().unlocked());
    assertTrue(response.unlock().levelSatisfied());
    assertEquals(first.getBadgeCode(), response.unlock().previousBadgeCode());
    assertFalse(response.unlock().previousBadgeSatisfied());
  }

  @Test
  void reportsInsufficientLevelSeparatelyFromBadge() {
    fixtures.grantBadge(userId, first.getBadgeCode());
    fixtures.changeLevel(userId, 1);

    GymLeaderResponse response = gymLeaderResponseOf(second);

    assertFalse(response.unlock().unlocked());
    assertFalse(response.unlock().levelSatisfied());
    assertEquals(3, response.unlock().requiredLevel());
    assertTrue(response.unlock().previousBadgeSatisfied());
  }

  @Test
  void unlocksWhenBothConditionsAreSatisfied() {
    fixtures.grantBadge(userId, first.getBadgeCode());
    fixtures.changeLevel(userId, 3);

    assertTrue(gymLeaderResponseOf(second).unlock().unlocked());
  }

  @Test
  void marksClearedByOwningGymLeaderBadge() {
    fixtures.grantBadge(userId, first.getBadgeCode());

    assertTrue(gymLeaderResponseOf(first).cleared());
    assertFalse(gymLeaderResponseOf(second).cleared());
  }

  @Test
  void exposesGymLeaderAnimalsWithoutSkillInformation() {
    GymLeaderDetailResponse response = gymLeaderQueryService.getGymLeader(userId, first.getId());

    assertEquals(BattlePolicy.ENTRY_COUNT, response.animals().size());
    List<String> componentNames =
        List.of(
                com.somagochi.pochakfarm.battle.dto.GymLeaderAnimalResponse.class
                    .getRecordComponents())
            .stream()
            .map(java.lang.reflect.RecordComponent::getName)
            .toList();
    assertFalse(componentNames.contains("skill1"));
    assertFalse(componentNames.contains("skill2"));
    assertFalse(componentNames.contains("triggerPercentage"));
    assertFalse(componentNames.contains("point"));
  }

  private GymLeaderResponse gymLeaderResponseOf(GymLeader gymLeader) {
    return gymLeaderQueryService.getGymLeaders(userId).stream()
        .filter(response -> response.gymLeaderId().equals(gymLeader.getId()))
        .findFirst()
        .orElseThrow();
  }
}
