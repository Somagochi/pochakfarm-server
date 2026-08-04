package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.DailyCaptureAttempt;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureStartRequest;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.DailyCaptureAttemptRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.domain.PresignedUpload;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CaptureStartConcurrencyTest {

  @Autowired private CaptureStartService captureStartService;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private DailyCaptureAttemptRepository attemptRepository;
  @Autowired private UserRepository userRepository;

  @MockitoBean private FileStorage fileStorage;

  private final ExecutorService executor = Executors.newFixedThreadPool(2);
  private Long userId;

  @BeforeEach
  void setUp() {
    attemptRepository.deleteAll();
    captureRepository.deleteAll();
    userRepository.deleteAll();
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO, "concurrency-" + UUID.randomUUID(), "concurrency@test.com"));
    userId = user.getId();
    attemptRepository.save(
        DailyCaptureAttempt.create(userId, LocalDate.now(ZoneId.of("Asia/Seoul")), 1));
    for (int index = 0; index < 4; index++) {
      captureRepository.save(
          Capture.create(
              userId,
              UUID.randomUUID().toString(),
              CardType.GROUND,
              Tier.C,
              AnimalName.from("두부"),
              CardSkill.GROUND_PAW_STRIKE,
              CardSkill.GROUND_LEAF_GUARD,
              "001",
              "images/capture-original/%d/%s.jpg".formatted(userId, UUID.randomUUID()),
              "image/jpeg",
              Instant.now().plusSeconds(300)));
    }
    given(fileStorage.presignPut(any(), any(), any()))
        .willAnswer(
            invocation ->
                new PresignedUpload(
                    "https://upload.example/" + invocation.getArgument(0),
                    Instant.now().plusSeconds(300)));
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void onlyOneConcurrentRequestConsumesTheLastAttempt() throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Callable<String>> requests = new ArrayList<>();
    for (int index = 0; index < 2; index++) {
      requests.add(
          () -> {
            ready.countDown();
            start.await(5, TimeUnit.SECONDS);
            try {
              captureStartService.startCapture(
                  userId,
                  new CaptureStartRequest(UUID.randomUUID().toString(), "image/jpeg", "두부"));
              return "SUCCESS";
            } catch (BusinessException exception) {
              return exception.getCode();
            }
          });
    }

    List<Future<String>> results = new ArrayList<>();
    for (Callable<String> request : requests) {
      results.add(executor.submit(request));
    }
    ready.await(5, TimeUnit.SECONDS);
    start.countDown();

    List<String> outcomes = new ArrayList<>();
    for (Future<String> result : results) {
      outcomes.add(result.get(10, TimeUnit.SECONDS));
    }

    assertEquals(1, outcomes.stream().filter("SUCCESS"::equals).count());
    assertEquals(
        1, outcomes.stream().filter(ErrorCode.CAPTURE_ATTEMPT_REQUIRED.getCode()::equals).count());
    assertEquals(5, captureRepository.count());
  }
}
