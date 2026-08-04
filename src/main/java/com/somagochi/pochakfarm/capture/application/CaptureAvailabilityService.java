package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse.Attempts;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.DailyCaptureAttemptRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaptureAvailabilityService {

  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

  private final DailyCaptureAttemptRepository attemptRepository;
  private final UserRepository userRepository;
  private final Clock clock;

  public CaptureAvailabilityService(
      DailyCaptureAttemptRepository attemptRepository, UserRepository userRepository, Clock clock) {
    this.attemptRepository = attemptRepository;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public CaptureAvailabilityResponse getAvailability(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    LocalDate today = LocalDate.now(clock.withZone(KOREA_ZONE));
    int remaining =
        attemptRepository
            .findByUserIdAndAttemptDate(userId, today)
            .map(attempt -> attempt.getRemaining())
            .orElse(CaptureAttemptPurchaseService.DAILY_ATTEMPTS);
    return new CaptureAvailabilityResponse(
        new Attempts(remaining, today.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant()),
        CaptureAttemptPurchaseService.PURCHASE_COST,
        user.getCoins());
  }
}
