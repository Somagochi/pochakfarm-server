package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.BattleEntry;
import com.somagochi.pochakfarm.battle.domain.BattlePolicy;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "현재 출전 중인 동물")
public record BattleEntryResponse(
    @Schema(description = "진영", example = "USER") BattleSide side,
    @Schema(description = "출전 순번(1~3)", example = "1") int orderNo,
    @Schema(description = "포착 ID. NPC 동물이면 null", example = "10") Long captureId,
    @Schema(description = "동물 이름", example = "솜구름") String animalName,
    @Schema(description = "카드 타입", example = "SEA") CardType cardType,
    @Schema(description = "티어", example = "A") Tier tier,
    @Schema(description = "보유 스킬 2개. 관장 동물의 스킬은 공개하지 않으므로 NPC 진영은 null")
        List<BattleEntrySkillResponse> skills) {

  public static BattleEntryResponse from(BattleEntry entry, BattlePolicy battlePolicy) {
    return new BattleEntryResponse(
        entry.getSide(),
        entry.getOrderNo(),
        entry.getCaptureId(),
        entry.getAnimalName(),
        entry.getCardType(),
        entry.getTier(),
        entry.isUserSide()
            ? List.of(
                BattleEntrySkillResponse.of(entry.getSkill1(), battlePolicy),
                BattleEntrySkillResponse.of(entry.getSkill2(), battlePolicy))
            : null);
  }
}
