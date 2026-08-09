package com.somagochi.pochakfarm.capture.domain;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CaptureGameResultPolicy {

  private static final int MAX_THROWS = 3;

  public GameStatus resolve(List<CaptureThrow> throws_) {
    if (throws_ == null || throws_.isEmpty() || throws_.size() > MAX_THROWS) {
      throw invalidResult();
    }

    for (int index = 0; index < throws_.size(); index++) {
      CaptureThrow current = throws_.get(index);
      if (current == null
          || current.round() == null
          || current.round() != index + 1
          || current.succeeded() == null
          || (current.succeeded() && index != throws_.size() - 1)) {
        throw invalidResult();
      }
    }

    if (throws_.getLast().succeeded()) {
      return GameStatus.SUCCEEDED;
    }
    if (throws_.size() == MAX_THROWS) {
      return GameStatus.FAILED;
    }
    throw invalidResult();
  }

  private BusinessException invalidResult() {
    return new BusinessException(ErrorCode.INVALID_GAME_RESULT);
  }
}
