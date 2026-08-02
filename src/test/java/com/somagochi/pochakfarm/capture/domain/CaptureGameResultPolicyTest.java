package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CaptureGameResultPolicyTest {

  private final CaptureGameResultPolicy policy = new CaptureGameResultPolicy();

  @Test
  void succeedsWhenLastThrowSucceeds() {
    GameStatus result =
        policy.resolve(List.of(new CaptureThrow(1, false), new CaptureThrow(2, true)));

    assertEquals(GameStatus.SUCCEEDED, result);
  }

  @Test
  void failsAfterThreeFailedThrows() {
    GameStatus result =
        policy.resolve(
            List.of(
                new CaptureThrow(1, false),
                new CaptureThrow(2, false),
                new CaptureThrow(3, false)));

    assertEquals(GameStatus.FAILED, result);
  }

  @Test
  void rejectsEmptyThrows() {
    assertInvalid(List.of());
  }

  @Test
  void rejectsNullThrowOrRequiredFields() {
    assertInvalid(Collections.singletonList(null));
    assertInvalid(List.of(new CaptureThrow(null, true)));
    assertInvalid(List.of(new CaptureThrow(1, null)));
  }

  @Test
  void rejectsMoreThanThreeThrows() {
    assertInvalid(
        List.of(
            new CaptureThrow(1, false),
            new CaptureThrow(2, false),
            new CaptureThrow(3, false),
            new CaptureThrow(4, true)));
  }

  @Test
  void rejectsNonSequentialRounds() {
    assertInvalid(List.of(new CaptureThrow(1, false), new CaptureThrow(3, true)));
  }

  @Test
  void rejectsThrowsAfterSuccess() {
    assertInvalid(List.of(new CaptureThrow(1, true), new CaptureThrow(2, false)));
  }

  @Test
  void rejectsOneOrTwoFailuresAsIncompleteResult() {
    assertInvalid(List.of(new CaptureThrow(1, false), new CaptureThrow(2, false)));
  }

  private void assertInvalid(List<CaptureThrow> throws_) {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> policy.resolve(throws_));

    assertEquals(ErrorCode.INVALID_GAME_RESULT.getCode(), exception.getCode());
  }
}
