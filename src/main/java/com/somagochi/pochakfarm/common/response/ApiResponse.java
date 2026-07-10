package com.somagochi.pochakfarm.common.response;

import java.time.LocalDateTime;

public record ApiResponse<T>(T data, LocalDateTime datetime) {

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(data, LocalDateTime.now());
  }

  public static ApiResponse<Void> empty() {
    return success(null);
  }
}
