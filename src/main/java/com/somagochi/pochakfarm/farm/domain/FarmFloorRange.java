package com.somagochi.pochakfarm.farm.domain;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;

public record FarmFloorRange(int page, int firstSequence, int lastSequence) {

  public static FarmFloorRange ofPage(int page) {
    int firstSequence = page * FarmSpace.FLOOR_COUNT_PER_PAGE + 1;
    if (page < FarmSpace.FIRST_PAGE || firstSequence > FarmSpace.TOTAL_FLOOR_COUNT) {
      throw new BusinessException(ErrorCode.INVALID_PARAMETER);
    }
    int lastSequence =
        Math.min(firstSequence + FarmSpace.FLOOR_COUNT_PER_PAGE - 1, FarmSpace.TOTAL_FLOOR_COUNT);
    return new FarmFloorRange(page, firstSequence, lastSequence);
  }

  public int size() {
    return FarmSpace.FLOOR_COUNT_PER_PAGE;
  }

  public int totalPages() {
    return FarmSpace.TOTAL_PAGE_COUNT;
  }
}
