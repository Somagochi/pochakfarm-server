package com.somagochi.pochakfarm.battle.dto;

import com.somagochi.pochakfarm.battle.domain.BattleEntry;
import com.somagochi.pochakfarm.battle.domain.BattleSide;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "현재 출전 중인 관장 동물")
public record BattleStateNpcEntryResponse(
    @Schema(description = "진영", example = "NPC") BattleSide side,
    @Schema(description = "출전 순번(1~3)", example = "1") int orderNo,
    @Schema(description = "NPC 동물이므로 null") Long captureId,
    @Schema(description = "동물 이름", example = "별콩") String animalName,
    @Schema(description = "카드 타입", example = "SPACE") CardType cardType,
    @Schema(description = "티어", example = "B") Tier tier,
    @Schema(description = "보유 스킬 2개. 이름과 전투 유형만 공개한다") List<BattleNpcSkillResponse> skills) {

  public static BattleStateNpcEntryResponse from(BattleEntry entry) {
    return new BattleStateNpcEntryResponse(
        entry.getSide(),
        entry.getOrderNo(),
        entry.getCaptureId(),
        entry.getAnimalName(),
        entry.getCardType(),
        entry.getTier(),
        List.of(
            BattleNpcSkillResponse.from(entry.getSkill1()),
            BattleNpcSkillResponse.from(entry.getSkill2())));
  }
}
