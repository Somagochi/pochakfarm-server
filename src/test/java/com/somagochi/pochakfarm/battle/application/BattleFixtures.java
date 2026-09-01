package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
import com.somagochi.pochakfarm.badge.domain.Badge;
import com.somagochi.pochakfarm.badge.domain.UserBadge;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.BadgeRepository;
import com.somagochi.pochakfarm.badge.infrastructure.persistence.UserBadgeRepository;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.domain.GymLeaderAnimal;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.farm.application.FarmInitializationService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BattleFixtures {

  private final UserRepository userRepository;
  private final CaptureRepository captureRepository;
  private final AnimalRepository animalRepository;
  private final FarmSpaceRepository farmSpaceRepository;
  private final FarmInitializationService farmInitializationService;
  private final GymLeaderRepository gymLeaderRepository;
  private final GymLeaderAnimalRepository gymLeaderAnimalRepository;
  private final BadgeRepository badgeRepository;
  private final UserBadgeRepository userBadgeRepository;
  private final JdbcTemplate jdbcTemplate;

  public Long createUser() {
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                "battle-" + UUID.randomUUID(),
                "battle-" + UUID.randomUUID() + "@test.com",
                "u" + UUID.randomUUID().toString().substring(0, 5)));
    farmInitializationService.initialize(user.getId());
    return user.getId();
  }

  public void changeLevel(Long userId, int level) {
    jdbcTemplate.update("update users set level = ? where id = ?", level, userId);
  }

  public Animal createAnimal(Long userId, CardType cardType, Tier tier, CardSkill skill) {
    Capture capture =
        captureRepository.saveAndFlush(
            Capture.create(
                userId,
                UUID.randomUUID().toString(),
                cardType,
                tier,
                AnimalName.from("동물" + UUID.randomUUID().toString().substring(0, 4)),
                skill,
                skill,
                "123",
                "images/capture-original/%d/%s.jpg".formatted(userId, UUID.randomUUID()),
                "image/jpeg",
                Instant.parse("2026-08-03T01:05:00Z")));
    capture.succeed("public/capture-animal/a.png", "public/capture-card/card.png", 100);
    capture.completeGame(GameStatus.SUCCEEDED, 10);
    captureRepository.saveAndFlush(capture);
    FarmSpace space = farmSpaceRepository.findByUserIdAndType(userId, cardType).orElseThrow();
    return animalRepository.saveAndFlush(Animal.create(capture.getId(), space.getId(), 0, 0));
  }

  public void markResting(Long animalId, Instant restEndsAt) {
    jdbcTemplate.update("update animals set rest_ends_at = ? where id = ?", restEndsAt, animalId);
  }

  public GymLeader createGymLeader(int challengeOrder, int animalCount) {
    String badgeCode = "BDG%03d".formatted(100 + challengeOrder);
    badgeRepository.saveAndFlush(
        Badge.create(badgeCode, "뱃지" + challengeOrder, "설명" + challengeOrder, null));
    GymLeader gymLeader =
        gymLeaderRepository.saveAndFlush(
            GymLeader.create(
                "GYM%03d".formatted(challengeOrder),
                "관장" + challengeOrder,
                challengeOrder,
                badgeCode,
                null));
    for (int orderNo = 1; orderNo <= animalCount; orderNo++) {
      gymLeaderAnimalRepository.saveAndFlush(
          GymLeaderAnimal.create(
              gymLeader.getId(),
              orderNo,
              AnimalName.from("관장동물" + challengeOrder + orderNo),
              CardType.GROUND,
              Tier.C,
              CardSkill.GROUND_LEAF_GUARD,
              CardSkill.GROUND_STONE_TAP,
              null));
    }
    return gymLeader;
  }

  public void grantBadge(Long userId, String badgeCode) {
    Badge badge = badgeRepository.findByCode(badgeCode).orElseThrow();
    userBadgeRepository.saveAndFlush(UserBadge.acquire(userId, badge.getId()));
  }

  public void cleanUp() {
    jdbcTemplate.update("delete from battle_entries");
    jdbcTemplate.update("delete from battles");
    jdbcTemplate.update("delete from gym_leader_animals");
    jdbcTemplate.update("delete from gym_leaders");
    jdbcTemplate.update("delete from user_badges");
    jdbcTemplate.update("delete from badges");
    jdbcTemplate.update("delete from animals");
    jdbcTemplate.update("delete from captures");
    jdbcTemplate.update("delete from farm_spaces");
    jdbcTemplate.update("delete from users");
  }
}
