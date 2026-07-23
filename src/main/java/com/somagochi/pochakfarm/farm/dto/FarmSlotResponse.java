package com.somagochi.pochakfarm.farm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "층에 속한 슬롯 정보")
public record FarmSlotResponse(
    @Schema(description = "슬롯 ID. 슬롯이 존재하지 않으면 null", example = "1001") Long slotId,
    @Schema(description = "슬롯 순번(1부터 시작)", example = "1") Integer sequence,
    @Schema(description = "배치된 동물. 비어 있으면 null") FarmAnimalResponse animal) {

  public static FarmSlotResponse empty(Long slotId, Integer sequence) {
    return new FarmSlotResponse(slotId, sequence, null);
  }

  public static FarmSlotResponse occupied(
      Long slotId, Integer sequence, FarmAnimalResponse animal) {
    return new FarmSlotResponse(slotId, sequence, animal);
  }
}
