package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse;
import com.somagochi.pochakfarm.capture.dto.CaptureAvailabilityResponse.FreeAttempts;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaptureAvailabilityService {

  private static final int DAILY_LIMIT = 5;
  private static final long EXTRA_CAPTURE_COST = 200L;
  private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

  private final CaptureRepository captureRepository;
  private final UserRepository userRepository;
  private final Clock clock;

  public CaptureAvailabilityService(
      CaptureRepository captureRepository, UserRepository userRepository, Clock clock) {
    this.captureRepository = captureRepository;
    this.userRepository = userRepository;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public CaptureAvailabilityResponse getAvailability(Long userId) {
    User user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    DailyRange dailyRange = currentDailyRange();
    long usedCount =
        captureRepository.countFreeAttemptsByUserIdBetween(
            userId, dailyRange.startInclusive(), dailyRange.endExclusive());
    int used = Math.toIntExact(Math.min(usedCount, DAILY_LIMIT));
    int remaining = Math.max(DAILY_LIMIT - used, 0);
    boolean canStartCapture = remaining > 0 || user.getCoins() >= EXTRA_CAPTURE_COST;
    return new CaptureAvailabilityResponse(
        new FreeAttempts(DAILY_LIMIT, used, remaining, dailyRange.endExclusive()),
        EXTRA_CAPTURE_COST,
        user.getCoins(),
        canStartCapture);
  }

  private DailyRange currentDailyRange() {
    LocalDate today = LocalDate.now(clock.withZone(KOREA_ZONE));
    return new DailyRange(
        today.atStartOfDay(KOREA_ZONE).toInstant(),
        today.plusDays(1).atStartOfDay(KOREA_ZONE).toInstant());
  }

  private record DailyRange(Instant startInclusive, Instant endExclusive) {}
}
