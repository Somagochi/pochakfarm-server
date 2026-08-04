package com.somagochi.pochakfarm.capture.application;

import com.somagochi.pochakfarm.capture.domain.CaptureAttemptPurchase;
import com.somagochi.pochakfarm.capture.domain.DailyCaptureAttempt;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureAttemptPurchaseRepository;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.DailyCaptureAttemptRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.application.UserCoinService;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CaptureAttemptPurchaseService {

  static final int DAILY_ATTEMPTS = 5;
  static final long PURCHASE_COST = 200L;
  static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

  private final CaptureAttemptPurchaseRepository purchaseRepository;
  private final DailyCaptureAttemptRepository attemptRepository;
  private final UserRepository userRepository;
  private final UserCoinService userCoinService;
  private final Clock clock;

  public CaptureAttemptPurchaseService(
      CaptureAttemptPurchaseRepository purchaseRepository,
      DailyCaptureAttemptRepository attemptRepository,
      UserRepository userRepository,
      UserCoinService userCoinService,
      Clock clock) {
    this.purchaseRepository = purchaseRepository;
    this.attemptRepository = attemptRepository;
    this.userRepository = userRepository;
    this.userCoinService = userCoinService;
    this.clock = clock;
  }

  @Transactional
  public CaptureAttemptPurchaseResponse purchase(
      Long userId, CaptureAttemptPurchaseRequest request) {
    String clientRequestId = normalizeClientRequestId(request.clientRequestId());
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    LocalDate today = today();
    DailyCaptureAttempt attempt = currentAttemptForUpdate(userId, today);
    if (purchaseRepository.findByUserIdAndClientRequestId(userId, clientRequestId).isPresent()) {
      return response(attempt, user);
    }

    attempt.purchase();
    CaptureAttemptPurchase purchase =
        purchaseRepository.saveAndFlush(CaptureAttemptPurchase.create(userId, clientRequestId));
    User charged =
        userCoinService.spend(
            userId,
            PURCHASE_COST,
            CoinTransactionReason.CAPTURE_ATTEMPT_PURCHASE,
            purchase.getId());
    return response(attempt, charged);
  }

  private DailyCaptureAttempt currentAttemptForUpdate(Long userId, LocalDate today) {
    return attemptRepository
        .findByUserIdAndAttemptDateForUpdate(userId, today)
        .orElseGet(
            () ->
                attemptRepository.save(DailyCaptureAttempt.create(userId, today, DAILY_ATTEMPTS)));
  }

  private CaptureAttemptPurchaseResponse response(DailyCaptureAttempt attempt, User user) {
    return new CaptureAttemptPurchaseResponse(
        attempt.getRemaining(),
        PURCHASE_COST,
        user.getCoins(),
        attempt.getAttemptDate().plusDays(1).atStartOfDay(KOREA_ZONE).toInstant());
  }

  private LocalDate today() {
    return LocalDate.now(clock.withZone(KOREA_ZONE));
  }

  private String normalizeClientRequestId(String clientRequestId) {
    try {
      UUID parsed = UUID.fromString(clientRequestId);
      if (!parsed.toString().equalsIgnoreCase(clientRequestId)) {
        throw new IllegalArgumentException();
      }
      return parsed.toString();
    } catch (IllegalArgumentException | NullPointerException exception) {
      throw new BusinessException(ErrorCode.INVALID_CLIENT_REQUEST_ID);
    }
  }
}
