package com.somagochi.pochakfarm.farm.dto;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.farm.domain.FarmFloorRange;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "테마별 농장 페이지 정보")
public record FarmSpaceResponse(
    @Schema(description = "농장 테마", example = "SEA") CardType type,
    @Schema(description = "현재 페이지(0부터 시작)", example = "0") int page,
    @Schema(description = "한 페이지에 포함되는 층 수", example = "4") int size,
    @Schema(description = "총 페이지 수", example = "1") int totalPages,
    @Schema(description = "현재 페이지의 층 목록. 층 순번 오름차순") List<FarmFloorResponse> floors) {

  public static FarmSpaceResponse of(
      CardType type, FarmFloorRange range, List<FarmFloorResponse> floors) {
    return new FarmSpaceResponse(type, range.page(), range.size(), range.totalPages(), floors);
  }
}
