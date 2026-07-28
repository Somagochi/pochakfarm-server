package com.somagochi.pochakfarm.farm.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "농장 층 정보")
public record FarmFloorResponse(
    @Schema(description = "층 순번(1부터 시작)", example = "1") Integer sequence,
    @Schema(description = "개방 여부", example = "true") boolean unlocked,
    @Schema(description = "슬롯 목록. 미개방 층은 빈 배열") List<FarmSlotResponse> slots) {

  public static FarmFloorResponse locked(Integer sequence) {
    return new FarmFloorResponse(sequence, false, List.of());
  }

  public static FarmFloorResponse unlocked(Integer sequence, List<FarmSlotResponse> slots) {
    return new FarmFloorResponse(sequence, true, slots);
  }
}
