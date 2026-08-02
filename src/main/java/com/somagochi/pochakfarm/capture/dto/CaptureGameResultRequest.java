package com.somagochi.pochakfarm.capture.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.somagochi.pochakfarm.capture.domain.CaptureThrow;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

public record CaptureGameResultRequest(
    @JsonProperty("throws") @Schema(description = "1부터 순차적으로 제출하는 최대 3회의 투척 결과")
        List<ThrowResult> throwResults) {

  public List<CaptureThrow> toDomain() {
    if (throwResults == null) {
      return null;
    }
    return throwResults.stream()
        .map(result -> result == null ? null : new CaptureThrow(result.round(), result.succeeded()))
        .toList();
  }

  public record ThrowResult(
      @Schema(description = "1부터 시작하는 투척 순서", example = "1") Integer round,
      @Schema(description = "클라이언트가 확정한 투척 성공 여부", example = "false") Boolean succeeded) {}
}
