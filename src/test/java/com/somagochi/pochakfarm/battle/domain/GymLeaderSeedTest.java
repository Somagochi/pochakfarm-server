package com.somagochi.pochakfarm.battle.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.capture.domain.Tier;
import java.sql.Connection;
import java.util.List;
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
      assertNull(leader.getImageKey());
    }
    assertEquals("흙담이", leaders.get(0).getName());
    assertEquals("온누리", leaders.get(LEADER_COUNT - 1).getName());
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
        assertNotEquals(animal.getSkill1().battleType(), animal.getSkill2().battleType());
        assertNull(animal.getImageKey());
      }
    }
    assertEquals(LEADER_COUNT * GymLeader.ANIMAL_COUNT, gymLeaderAnimalRepository.findAll().size());
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
