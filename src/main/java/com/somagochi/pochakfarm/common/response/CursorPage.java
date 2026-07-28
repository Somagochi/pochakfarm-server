package com.somagochi.pochakfarm.common.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "커서 기반 페이지")
public record CursorPage<T>(
    @Schema(description = "현재 페이지 항목") List<T> content,
    @Schema(description = "다음 페이지 커서. 없으면 null", example = "38") Long nextCursor,
    @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext) {

  public static <T> CursorPage<T> of(List<T> content, Long nextCursor, boolean hasNext) {
    return new CursorPage<>(content, nextCursor, hasNext);
  }
}
