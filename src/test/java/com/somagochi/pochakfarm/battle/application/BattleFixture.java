package com.somagochi.pochakfarm.battle.application;

import com.somagochi.pochakfarm.battle.domain.Battle;
import com.somagochi.pochakfarm.battle.domain.BattleEntry;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.GymLeader;
import com.somagochi.pochakfarm.battle.domain.GymLeaderAnimal;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleEntryRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.BattleRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderAnimalRepository;
import com.somagochi.pochakfarm.battle.infrastructure.persistence.GymLeaderRepository;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

final class BattleFixture {

  static final CardSkill USER_STABLE_SKILL = CardSkill.SEA_BUBBLE_GUARD;
  static final CardSkill USER_GAMBLE_SKILL = CardSkill.SEA_WAVE_DASH;
  static final CardSkill NPC_STABLE_SKILL = CardSkill.SEA_SEASHELL_SHIELD;
  static final CardSkill NPC_GAMBLE_SKILL = CardSkill.SEA_SPLASH_PAW;

  private static final AtomicInteger CHALLENGE_ORDER = new AtomicInteger();

  private final BattleRepository battleRepository;
  private final BattleEntryRepository battleEntryRepository;
  private final GymLeaderRepository gymLeaderRepository;
  private final GymLeaderAnimalRepository gymLeaderAnimalRepository;

  private Tier userTier = Tier.A;
  private Tier npcTier = Tier.A;
  private CardType userCardType = CardType.SEA;
  private CardType npcCardType = CardType.SEA;
  private CardSkill userSkill1 = USER_STABLE_SKILL;
  private CardSkill userSkill2 = USER_GAMBLE_SKILL;
  private CardSkill npcSkill1 = NPC_STABLE_SKILL;
  private CardSkill npcSkill2 = NPC_GAMBLE_SKILL;
  private int barPosition;
  private Integer challengeOrder;

  BattleFixture(
      BattleRepository battleRepository,
      BattleEntryRepository battleEntryRepository,
      GymLeaderRepository gymLeaderRepository,
      GymLeaderAnimalRepository gymLeaderAnimalRepository) {
    this.battleRepository = battleRepository;
    this.battleEntryRepository = battleEntryRepository;
    this.gymLeaderRepository = gymLeaderRepository;
    this.gymLeaderAnimalRepository = gymLeaderAnimalRepository;
  }

  BattleFixture tiers(Tier userTier, Tier npcTier) {
    this.userTier = userTier;
    this.npcTier = npcTier;
    return this;
  }

  BattleFixture cardTypes(CardType userCardType, CardType npcCardType) {
    this.userCardType = userCardType;
    this.npcCardType = npcCardType;
    return this;
  }

  BattleFixture userSkills(CardSkill skill1, CardSkill skill2) {
    this.userSkill1 = skill1;
    this.userSkill2 = skill2;
    return this;
  }

  BattleFixture npcSkills(CardSkill skill1, CardSkill skill2) {
    this.npcSkill1 = skill1;
    this.npcSkill2 = skill2;
    return this;
  }

  BattleFixture barPosition(int barPosition) {
    this.barPosition = barPosition;
    return this;
  }

  BattleFixture challengeOrder(int challengeOrder) {
    this.challengeOrder = challengeOrder;
    return this;
  }

  Battle start(Long userId, Instant startedAt) {
    GymLeader gymLeader = gymLeaderRepository.save(gymLeader());
    Battle battle =
        battleRepository.save(
            Battle.start(userId, gymLeader.getId(), UUID.randomUUID().toString(), startedAt));
    if (barPosition != BattlePolicy.INITIAL_BAR_POSITION) {
      battle.applyAction(barPosition, startedAt);
      battleRepository.save(battle);
    }
    for (int orderNo = 1; orderNo <= BattleEntry.ENTRY_COUNT; orderNo++) {
      battleEntryRepository.save(
          BattleEntry.ofUser(
              battle.getId(),
              orderNo,
              (long) orderNo,
              AnimalName.from("유저" + orderNo),
              userCardType,
              userTier,
              userSkill1,
              userSkill2));
      battleEntryRepository.save(
          BattleEntry.ofNpc(battle.getId(), gymLeaderAnimal(gymLeader.getId(), orderNo)));
    }
    return battle;
  }

  private GymLeaderAnimal gymLeaderAnimal(Long gymLeaderId, int orderNo) {
    return gymLeaderAnimalRepository.save(
        GymLeaderAnimal.create(
            gymLeaderId,
            orderNo,
            AnimalName.from("관장" + orderNo),
            npcCardType,
            npcTier,
            npcSkill1,
            npcSkill2,
            null));
  }

  private GymLeader gymLeader() {
    int resolvedChallengeOrder =
        challengeOrder == null
            ? CHALLENGE_ORDER.updateAndGet(
                previous -> previous >= GymLeader.LAST_CHALLENGE_ORDER ? 1 : previous + 1)
            : challengeOrder;
    String unique = UUID.randomUUID().toString();
    return GymLeader.create(
        "leader-" + unique, "관장", resolvedChallengeOrder, "badge-" + unique, null, null);
  }
}
