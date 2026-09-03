package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.SkillBattleType;
import java.sql.Connection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

@SpringBootTest
class GymLeaderSeedTest {

  private static final String SEED_SCRIPT = "db/gym-leader-seed.sql";
  private static final int LEADER_COUNT = 8;
  private static final int BADGE_COUNT = 8;
  private static final List<Tier> GYM008_TIERS = List.of(Tier.S, Tier.SS, Tier.SSS);

  // 관장별 스킬 전투 유형 슬롯 구성 (동물 3마리 x 스킬 2개 = 6슬롯). SOMA-206 난이도표의 확정값이다.
  // 슬롯 구성을 맞추느라 스킬 2개가 같은 유형인 동물이 있어, 동물 단위로 유형 상이를 요구하지 않는다.
  private static final Map<String, List<Integer>> SKILL_SLOT_COMPOSITIONS =
      Map.of(
          "GYM001", List.of(5, 1, 0),
          "GYM002", List.of(4, 2, 0),
          "GYM003", List.of(3, 3, 0),
          "GYM004", List.of(2, 4, 0),
          "GYM005", List.of(2, 3, 1),
          "GYM006", List.of(1, 4, 1),
          "GYM007", List.of(1, 3, 2),
          "GYM008", List.of(0, 3, 3));

  @Autowired private DataSource dataSource;
  @Autowired private GymLeaderRepository gymLeaderRepository;
  @Autowired private GymLeaderAnimalRepository gymLeaderAnimalRepository;
  @Autowired private BadgeRepository badgeRepository;

  @BeforeEach
  void setUp() throws Exception {
    gymLeaderAnimalRepository.deleteAllInBatch();
    gymLeaderRepository.deleteAllInBatch();
    badgeRepository.deleteAllInBatch();
    runSeedScript(false);
  }

  @Test
  void loadsEightGymLeadersInChallengeOrder() {
    List<GymLeader> leaders = gymLeaderRepository.findAllByOrderByChallengeOrderAsc();

    assertEquals(LEADER_COUNT, leaders.size());
    for (int index = 0; index < LEADER_COUNT; index++) {
      GymLeader leader = leaders.get(index);
      assertEquals("GYM%03d".formatted(index + 1), leader.getCode());
      assertEquals(index + 1, leader.getChallengeOrder());
      assertEquals("BDG%03d".formatted(index + 6), leader.getBadgeCode());
      assertNull(leader.getThumbnailKey());
      assertNull(leader.getImageKey());
    }
    assertEquals("두더", leaders.get(0).getName());
    assertEquals("아스트라", leaders.get(LEADER_COUNT - 1).getName());
  }

  @Test
  void loadsEightBadges() {
    for (int number = 6; number < 6 + BADGE_COUNT; number++) {
      String code = "BDG%03d".formatted(number);
      assertTrue(badgeRepository.findByCode(code).isPresent(), code);
    }
  }

  @Test
  void loadsThreeAnimalsPerGymLeaderWithConsistentSkills() {
    for (GymLeader leader : gymLeaderRepository.findAllByOrderByChallengeOrderAsc()) {
      List<GymLeaderAnimal> animals =
          gymLeaderAnimalRepository.findByGymLeaderIdOrderByOrderNoAsc(leader.getId());

      assertEquals(GymLeader.ANIMAL_COUNT, animals.size(), leader.getCode());
      for (int index = 0; index < animals.size(); index++) {
        GymLeaderAnimal animal = animals.get(index);
        assertEquals(index + 1, animal.getOrderNo());
        assertEquals(animal.getCardType(), animal.getSkill1().cardType());
        assertEquals(animal.getCardType(), animal.getSkill2().cardType());
        assertNull(animal.getImageKey());
      }
    }
    assertEquals(LEADER_COUNT * GymLeader.ANIMAL_COUNT, gymLeaderAnimalRepository.findAll().size());
  }

  @Test
  void keepsSkillSlotCompositionPerGymLeader() {
    for (GymLeader leader : gymLeaderRepository.findAllByOrderByChallengeOrderAsc()) {
      Map<SkillBattleType, Integer> counts = new EnumMap<>(SkillBattleType.class);
      for (SkillBattleType battleType : SkillBattleType.values()) {
        counts.put(battleType, 0);
      }
      for (GymLeaderAnimal animal :
          gymLeaderAnimalRepository.findByGymLeaderIdOrderByOrderNoAsc(leader.getId())) {
        counts.merge(animal.getSkill1().battleType(), 1, Integer::sum);
        counts.merge(animal.getSkill2().battleType(), 1, Integer::sum);
      }

      List<Integer> expected = SKILL_SLOT_COMPOSITIONS.get(leader.getCode());
      assertEquals(expected.get(0), counts.get(SkillBattleType.STABLE), leader.getCode());
      assertEquals(expected.get(1), counts.get(SkillBattleType.BALANCED), leader.getCode());
      assertEquals(expected.get(2), counts.get(SkillBattleType.GAMBLE), leader.getCode());
    }
  }

  @Test
  void keepsLastGymLeaderTierLadder() {
    GymLeader leader = gymLeaderRepository.findByCode("GYM008").orElseThrow();

    List<Tier> tiers =
        gymLeaderAnimalRepository.findByGymLeaderIdOrderByOrderNoAsc(leader.getId()).stream()
            .map(GymLeaderAnimal::getTier)
            .toList();

    assertEquals(GYM008_TIERS, tiers);
  }

  @Test
  void rerunDoesNotDuplicateGymLeaderAnimals() throws Exception {
    runSeedScript(true);

    assertEquals(LEADER_COUNT, gymLeaderRepository.findAll().size());
    assertEquals(LEADER_COUNT * GymLeader.ANIMAL_COUNT, gymLeaderAnimalRepository.findAll().size());
  }

  private void runSeedScript(boolean continueOnError) throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      ScriptUtils.executeSqlScript(
          connection,
          new EncodedResource(new ClassPathResource(SEED_SCRIPT), "UTF-8"),
          continueOnError,
          false,
          ScriptUtils.DEFAULT_COMMENT_PREFIX,
          ScriptUtils.DEFAULT_STATEMENT_SEPARATOR,
          ScriptUtils.DEFAULT_BLOCK_COMMENT_START_DELIMITER,
          ScriptUtils.DEFAULT_BLOCK_COMMENT_END_DELIMITER);
    }
  }
}
