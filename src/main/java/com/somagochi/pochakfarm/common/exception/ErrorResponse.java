package com.somagochi.pochakfarm.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

public record ErrorResponse(
    @Schema(description = "에러 발생 시각", example = "2026-07-07T12:34:56.789") LocalDateTime timestamp,
    @Schema(description = "HTTP 상태 코드", example = "404") int status,
    @Schema(description = "비즈니스 에러 코드", example = "USER_NOT_FOUND") String code,
    @Schema(description = "에러 메시지", example = "User not found") String message) {

  public static ErrorResponse of(int status, String code, String message) {
    return new ErrorResponse(LocalDateTime.now(), status, code, message);
  }
}
