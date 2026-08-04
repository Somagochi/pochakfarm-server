package com.somagochi.pochakfarm.capture.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DailyCaptureAttemptTest {

  @Test
  void consumesAndPurchasesAnAttempt() {
    DailyCaptureAttempt attempt = DailyCaptureAttempt.create(1L, LocalDate.of(2026, 8, 4), 1);

    attempt.consume();
    assertEquals(0, attempt.getRemaining());

    attempt.purchase();
    assertEquals(1, attempt.getRemaining());
  }

  @Test
  void rejectsConsumeWhenNoAttemptRemains() {
    DailyCaptureAttempt attempt = DailyCaptureAttempt.create(1L, LocalDate.of(2026, 8, 4), 0);

    BusinessException exception = assertThrows(BusinessException.class, attempt::consume);

    assertEquals(ErrorCode.CAPTURE_ATTEMPT_REQUIRED.getCode(), exception.getCode());
  }

  @Test
  void rejectsPurchaseWhenAttemptAlreadyRemains() {
    DailyCaptureAttempt attempt = DailyCaptureAttempt.create(1L, LocalDate.of(2026, 8, 4), 1);

    BusinessException exception = assertThrows(BusinessException.class, attempt::purchase);

    assertEquals(ErrorCode.CAPTURE_ATTEMPT_ALREADY_AVAILABLE.getCode(), exception.getCode());
  }
}
