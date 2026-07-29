package com.somagochi.pochakfarm.farm.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "층에 속한 슬롯 정보")
public record FarmSlotResponse(
    @Schema(description = "슬롯 번호(1부터 시작)", example = "1") Integer slotNum,
    @Schema(description = "배치된 동물. 비어 있으면 null") FarmAnimalResponse animal) {

  public static FarmSlotResponse empty(Integer slotNum) {
    return new FarmSlotResponse(slotNum, null);
  }

  public static FarmSlotResponse occupied(Integer slotNum, FarmAnimalResponse animal) {
    return new FarmSlotResponse(slotNum, animal);
  }
}
