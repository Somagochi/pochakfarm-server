package com.somagochi.pochakfarm.battle.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.domain.GymLeaderType;
import com.somagochi.pochakfarm.battle.dto.GymLeaderDetailResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderProfileResponse;
import com.somagochi.pochakfarm.battle.dto.GymLeaderResponse;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
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
    GymLeaderProfileResponse profile = gymLeaderProfileOf(first);

    assertTrue(profile.unlock().unlocked());
    assertEquals(1, profile.unlock().requiredLevel());
    assertTrue(profile.unlock().levelSatisfied());
    assertNull(profile.unlock().previousBadgeCode());
    assertTrue(profile.unlock().previousBadgeSatisfied());
    assertTrue(gymLeaderResponseOf(first).unlock().unlocked());
  }

  @Test
  void reportsMissingPreviousBadgeSeparatelyFromLevel() {
    fixtures.changeLevel(userId, 40);

    GymLeaderProfileResponse profile = gymLeaderProfileOf(second);

    assertFalse(profile.unlock().unlocked());
    assertTrue(profile.unlock().levelSatisfied());
    assertEquals(first.getBadgeCode(), profile.unlock().previousBadgeCode());
    assertFalse(profile.unlock().previousBadgeSatisfied());
    assertFalse(gymLeaderResponseOf(second).unlock().unlocked());
  }

  @Test
  void exposesSameUnlockConditionsInListAsInDetail() {
    fixtures.changeLevel(userId, 1);

    GymLeaderResponse listed = gymLeaderResponseOf(second);

    assertEquals(gymLeaderProfileOf(second).unlock(), listed.unlock());
    assertFalse(listed.unlock().levelSatisfied());
    assertEquals(first.getBadgeCode(), listed.unlock().previousBadgeCode());
    assertFalse(listed.unlock().previousBadgeSatisfied());
  }

  @Test
  void reportsInsufficientLevelSeparatelyFromBadge() {
    fixtures.grantBadge(userId, first.getBadgeCode());
    fixtures.changeLevel(userId, 1);

    GymLeaderProfileResponse profile = gymLeaderProfileOf(second);

    assertFalse(profile.unlock().unlocked());
    assertFalse(profile.unlock().levelSatisfied());
    assertEquals(3, profile.unlock().requiredLevel());
    assertTrue(profile.unlock().previousBadgeSatisfied());
  }

  @Test
  void unlocksWhenBothConditionsAreSatisfied() {
    fixtures.grantBadge(userId, first.getBadgeCode());
    fixtures.changeLevel(userId, 3);

    assertTrue(gymLeaderProfileOf(second).unlock().unlocked());
    assertTrue(gymLeaderResponseOf(second).unlock().unlocked());
  }

  @Test
  void exposesThumbnailInListAndImageInDetail() {
    fixtures.changeGymLeaderImages(
        first.getId(), "public/gym-leader-thumbnail/a.png", "public/gym-leader/a.png");

    assertTrue(
        gymLeaderResponseOf(first).thumbnailUrl().endsWith("public/gym-leader-thumbnail/a.png"));
    assertTrue(gymLeaderProfileOf(first).imageUrl().endsWith("public/gym-leader/a.png"));
  }

  @Test
  void exposesLeaderContentWithSuggestTypeCounteringLeaderType() {
    fixtures.changeGymLeaderContent(
        first.getId(), GymLeaderType.GROUND, "초보", "모루는 땅 타입만 데리고 나와요", "단단한 방어와 꾸준한 공격이 특징이에요");

    GymLeaderProfileResponse profile = gymLeaderProfileOf(first);

    assertEquals("땅", profile.leaderType());
    assertEquals("초보", profile.difficulty());
    assertEquals("모루는 땅 타입만 데리고 나와요", profile.leaderDescription());
    assertEquals("단단한 방어와 꾸준한 공격이 특징이에요", profile.tipDescription());
    assertEquals("하늘", profile.suggestType());
  }

  @Test
  void exposesMixedLabelAsSuggestTypeWhenLeaderTypeIsMixed() {
    fixtures.changeGymLeaderContent(
        first.getId(), GymLeaderType.MIXED, "중급", "라온은 여러 타입을 섞어 데리고 나와요", "자리마다 다른 타입으로 맞서세요");

    GymLeaderProfileResponse profile = gymLeaderProfileOf(first);

    assertEquals("복합", profile.leaderType());
    assertEquals("복합", profile.suggestType());
  }

  @Test
  void leavesLeaderContentNullWhenNotConfigured() {
    GymLeaderProfileResponse profile = gymLeaderProfileOf(second);

    assertNull(profile.leaderType());
    assertNull(profile.difficulty());
    assertNull(profile.leaderDescription());
    assertNull(profile.tipDescription());
    assertNull(profile.suggestType());
  }

  @Test
  void omitsDetailFieldsFromListResponse() {
    List<String> componentNames =
        List.of(GymLeaderResponse.class.getRecordComponents()).stream()
            .map(java.lang.reflect.RecordComponent::getName)
            .toList();

    assertFalse(componentNames.contains("code"));
    assertFalse(componentNames.contains("badgeCode"));
    assertFalse(componentNames.contains("imageUrl"));
    assertFalse(componentNames.contains("unlocked"));
  }

  @Test
  void exposesFirstClearRewardsByChallengeOrder() {
    GymLeaderProfileResponse firstProfile = gymLeaderProfileOf(first);
    GymLeaderProfileResponse secondProfile = gymLeaderProfileOf(second);

    assertEquals(300, firstProfile.coinReward());
    assertEquals(20, firstProfile.experienceReward());
    assertEquals(500, secondProfile.coinReward());
    assertEquals(30, secondProfile.experienceReward());
  }

  @Test
  void omitsBadgeCodeFromProfileResponse() {
    List<String> componentNames =
        List.of(GymLeaderProfileResponse.class.getRecordComponents()).stream()
            .map(java.lang.reflect.RecordComponent::getName)
            .toList();

    assertFalse(componentNames.contains("badgeCode"));
  }

  @Test
  void marksClearedByOwningGymLeaderBadge() {
    fixtures.grantBadge(userId, first.getBadgeCode());

    assertTrue(gymLeaderResponseOf(first).cleared());
    assertFalse(gymLeaderResponseOf(second).cleared());
  }

  @Test
  void exposesGymLeaderAnimalSkillNamesAndBattleTypes() {
    GymLeaderDetailResponse response = gymLeaderQueryService.getGymLeader(userId, first.getId());

    assertEquals(BattlePolicy.ENTRY_COUNT, response.animals().size());
    assertEquals(2, response.animals().getFirst().skills().size());
    assertEquals("나뭇잎 방어", response.animals().getFirst().skills().getFirst().name());
    assertEquals(
        CardSkill.GROUND_LEAF_GUARD.battleType(),
        response.animals().getFirst().skills().getFirst().battleType());
  }

  private GymLeaderResponse gymLeaderResponseOf(GymLeader gymLeader) {
    return gymLeaderQueryService.getGymLeaders(userId).stream()
        .filter(response -> response.gymLeaderId().equals(gymLeader.getId()))
        .findFirst()
        .orElseThrow();
  }

  private GymLeaderProfileResponse gymLeaderProfileOf(GymLeader gymLeader) {
    return gymLeaderQueryService.getGymLeader(userId, gymLeader.getId()).gymLeader();
  }
}
