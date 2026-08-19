package com.somagochi.pochakfarm.capture.application;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest;
import com.somagochi.pochakfarm.capture.dto.CaptureGameResultRequest.ThrowResult;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
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
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
class CaptureGameResultConcurrencyTest {

  @Autowired private CaptureGameResultService service;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private UserRepository userRepository;

  @MockitoBean private FileStorage fileStorage;

  private final ExecutorService executor = Executors.newFixedThreadPool(2);
  private Long userId;

  @BeforeEach
  void setUp() {
    captureRepository.deleteAll();
    userRepository.deleteAll();
    User user =
        User.register(
            SocialProvider.KAKAO,
            "game-result-" + UUID.randomUUID(),
            "game-result@test.com",
            "u" + UUID.randomUUID().toString().substring(0, 5));
    ReflectionTestUtils.setField(user, "experience", 39L);
    user = userRepository.save(user);
    userId = user.getId();
  }

  @AfterEach
  void tearDown() {
    executor.shutdownNow();
  }

  @Test
  void rewardsSameCaptureOnlyOnce() throws Exception {
    Capture capture = captureRepository.save(capture());

    List<GameStatus> results = runConcurrently(capture.getId(), capture.getId());

    assertEquals(List.of(GameStatus.SUCCEEDED, GameStatus.SUCCEEDED), results);
    User rewarded = userRepository.findById(userId).orElseThrow();
    assertEquals(2, rewarded.getLevel());
    assertEquals(9, rewarded.getExperience());
    assertEquals(1_500, rewarded.getCoins());
  }

  @Test
  void accumulatesRewardsFromDifferentCapturesForSameUser() throws Exception {
    Capture first = captureRepository.save(capture());
    Capture second = captureRepository.save(capture());

    List<GameStatus> results = runConcurrently(first.getId(), second.getId());

    assertEquals(List.of(GameStatus.SUCCEEDED, GameStatus.SUCCEEDED), results);
    User rewarded = userRepository.findById(userId).orElseThrow();
    assertEquals(2, rewarded.getLevel());
    assertEquals(19, rewarded.getExperience());
    assertEquals(1_500, rewarded.getCoins());
  }

  private List<GameStatus> runConcurrently(Long firstCaptureId, Long secondCaptureId)
      throws Exception {
    CountDownLatch ready = new CountDownLatch(2);
    CountDownLatch start = new CountDownLatch(1);
    List<Callable<GameStatus>> requests =
        List.of(request(firstCaptureId, ready, start), request(secondCaptureId, ready, start));
    List<Future<GameStatus>> futures =
        List.of(executor.submit(requests.get(0)), executor.submit(requests.get(1)));
    ready.await(5, TimeUnit.SECONDS);
    start.countDown();
    return List.of(
        futures.get(0).get(10, TimeUnit.SECONDS), futures.get(1).get(10, TimeUnit.SECONDS));
  }

  private Callable<GameStatus> request(Long captureId, CountDownLatch ready, CountDownLatch start) {
    return () -> {
      ready.countDown();
      start.await(5, TimeUnit.SECONDS);
      return service
          .submit(
              userId, captureId, new CaptureGameResultRequest(List.of(new ThrowResult(1, true))))
          .gameStatus();
    };
  }

  private Capture capture() {
    return Capture.create(
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
        Instant.now().plusSeconds(300));
  }
}
