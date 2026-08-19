package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.somagochi.pochakfarm.capture.domain.DailyCaptureAttempt;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureAttemptPurchaseResponse;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureAttemptPurchaseRepository;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.DailyCaptureAttemptRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.Coin;
import com.somagochi.pochakfarm.user.domain.CoinTransactionReason;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.CoinHistoryRepository;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class CaptureAttemptPurchaseServiceIntegrationTest {

  @Autowired private CaptureAttemptPurchaseService service;
  @Autowired private CaptureAttemptPurchaseRepository purchaseRepository;
  @Autowired private DailyCaptureAttemptRepository attemptRepository;
  @Autowired private CoinHistoryRepository coinHistoryRepository;
  @Autowired private UserRepository userRepository;
  private Long userId;
  private LocalDate today;

  @BeforeEach
  void setUp() {
    purchaseRepository.deleteAll();
    attemptRepository.deleteAll();
    coinHistoryRepository.deleteAll();
    userRepository.deleteAll();
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                UUID.randomUUID().toString(),
                "purchase@test.com",
                "u" + UUID.randomUUID().toString().substring(0, 5)));
    userId = user.getId();
    today = LocalDate.now(ZoneId.of("Asia/Seoul"));
  }

  @Test
  void purchasesOneAttemptAndRecordsCoinHistory() {
    attemptRepository.save(DailyCaptureAttempt.create(userId, today, 0));

    CaptureAttemptPurchaseResponse response =
        service.purchase(userId, new CaptureAttemptPurchaseRequest(UUID.randomUUID().toString()));

    assertEquals(1, response.remaining());
    assertEquals(200, response.chargedCoins());
    assertEquals(800, response.currentCoins());
    assertEquals(1, coinHistoryRepository.count());
    assertEquals(
        CoinTransactionReason.CAPTURE_ATTEMPT_PURCHASE,
        coinHistoryRepository.findAll().getFirst().getReason());
  }

  @Test
  void sameRequestNeverChargesOrCreditsAgain() {
    attemptRepository.save(DailyCaptureAttempt.create(userId, today, 0));
    String requestId = UUID.randomUUID().toString();
    service.purchase(userId, new CaptureAttemptPurchaseRequest(requestId));
    DailyCaptureAttempt attempt =
        attemptRepository.findByUserIdAndAttemptDate(userId, today).orElseThrow();
    attempt.consume();
    attemptRepository.saveAndFlush(attempt);

    CaptureAttemptPurchaseResponse retry =
        service.purchase(userId, new CaptureAttemptPurchaseRequest(requestId));

    assertEquals(0, retry.remaining());
    assertEquals(800, retry.currentCoins());
    assertEquals(1, coinHistoryRepository.count());
  }

  @Test
  void rejectsPurchaseWhileAttemptRemains() {
    attemptRepository.save(DailyCaptureAttempt.create(userId, today, 1));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.purchase(
                    userId, new CaptureAttemptPurchaseRequest(UUID.randomUUID().toString())));

    assertEquals(ErrorCode.CAPTURE_ATTEMPT_ALREADY_AVAILABLE.getCode(), exception.getCode());
    assertEquals(1000, userRepository.findById(userId).orElseThrow().getCoins());
  }

  @Test
  void insufficientCoinsRollBackPurchaseAndAttemptCredit() {
    User user = userRepository.findById(userId).orElseThrow();
    ReflectionTestUtils.setField(user, "coins", Coin.of(100));
    userRepository.saveAndFlush(user);
    attemptRepository.save(DailyCaptureAttempt.create(userId, today, 0));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.purchase(
                    userId, new CaptureAttemptPurchaseRequest(UUID.randomUUID().toString())));

    assertEquals(ErrorCode.INSUFFICIENT_COINS.getCode(), exception.getCode());
    assertEquals(
        0,
        attemptRepository.findByUserIdAndAttemptDate(userId, today).orElseThrow().getRemaining());
    assertEquals(0, purchaseRepository.count());
    assertEquals(0, coinHistoryRepository.count());
  }

  @Test
  void rejectsInvalidClientRequestIdBeforeLockingUser() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.purchase(userId, new CaptureAttemptPurchaseRequest("not-a-uuid")));

    assertEquals(ErrorCode.INVALID_CLIENT_REQUEST_ID.getCode(), exception.getCode());
    assertEquals(0, purchaseRepository.count());
  }

  @Test
  void concurrentRetriesWithSameRequestChargeOnlyOnce() throws Exception {
    attemptRepository.save(DailyCaptureAttempt.create(userId, today, 0));
    String requestId = UUID.randomUUID().toString();
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService executor = Executors.newFixedThreadPool(2);
    try {
      java.util.concurrent.Callable<CaptureAttemptPurchaseResponse> request =
          () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            return service.purchase(userId, new CaptureAttemptPurchaseRequest(requestId));
          };
      Future<CaptureAttemptPurchaseResponse> first = executor.submit(request);
      Future<CaptureAttemptPurchaseResponse> second = executor.submit(request);
      ready.await(5, TimeUnit.SECONDS);
      start.countDown();

      assertEquals(1, first.get(10, TimeUnit.SECONDS).remaining());
      assertEquals(1, second.get(10, TimeUnit.SECONDS).remaining());
      assertEquals(1, purchaseRepository.count());
      assertEquals(1, coinHistoryRepository.count());
      assertEquals(800, userRepository.findById(userId).orElseThrow().getCoins());
    } finally {
      executor.shutdownNow();
    }
  }
}
