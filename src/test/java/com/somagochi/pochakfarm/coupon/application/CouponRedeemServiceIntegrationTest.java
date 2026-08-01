package com.somagochi.pochakfarm.coupon.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.capture.domain.Capture;
import com.somagochi.pochakfarm.capture.domain.GameStatus;
import com.somagochi.pochakfarm.capture.domain.GenerationStatus;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.capture.infrastructure.persistence.CaptureRepository;
import com.somagochi.pochakfarm.characterization.domain.AnimalName;
import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.Characterization;
import com.somagochi.pochakfarm.characterization.infrastructure.persistence.CharacterizationRepository;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.domain.CouponStatus;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.coupon.dto.CouponCompleteResponse;
import com.somagochi.pochakfarm.coupon.dto.CouponRedeemResponse;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.CouponRepository;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.infrastructure.InMemoryFileStorage;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
@Import(CouponRedeemServiceIntegrationTest.TestFileStorageConfig.class)
class CouponRedeemServiceIntegrationTest {

  @TestConfiguration
  static class TestFileStorageConfig {

    @Bean
    @Primary
    FileStorage inMemoryFileStorage() {
      return new InMemoryFileStorage();
    }
  }

  @Autowired private CouponRedeemService couponRedeemService;
  @Autowired private CouponCompleteService couponCompleteService;
  @Autowired private CouponRepository couponRepository;
  @Autowired private PreRegistrationCouponRecipientRepository recipientRepository;
  @Autowired private PreRegistrationRepository preRegistrationRepository;
  @Autowired private CharacterizationRepository characterizationRepository;
  @Autowired private CaptureRepository captureRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private FileStorage fileStorage;
  @Autowired private JdbcTemplate jdbcTemplate;

  private Long userId;
  private long initialCoins;

  @BeforeEach
  void setUp() {
    cleanUp();
    User user = persistUser();
    userId = user.getId();
    initialCoins = user.getCoins();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @Test
  void redeemsCouponAndMigratesCardWithoutUsingCoupon() {
    Coupon coupon =
        persistCouponWithRecipient("TEST-CODE-1", Instant.now().plus(Duration.ofDays(1)));

    CouponRedeemResponse response = couponRedeemService.redeem(userId, "TEST-CODE-1");

    assertEquals("두부", response.animalName());
    assertEquals(CardType.GROUND, response.cardType());
    assertEquals(Tier.S, response.tier());
    assertEquals("123", response.cardNo());
    assertNotNull(response.cardImageUrl());
    assertNotNull(response.animalImageUpload().uploadUrl());
    assertTrue(response.animalImageUpload().key().contains("/" + userId + "/"));

    Capture capture = captureRepository.findById(response.captureId()).orElseThrow();
    assertTrue(capture.isOwnedBy(userId));
    assertEquals(GenerationStatus.PROCESSING, capture.getGenerationStatus());
    assertEquals(GameStatus.PENDING, capture.getGameStatus());

    assertEquals(
        CouponStatus.ACTIVE, couponRepository.findById(coupon.getId()).orElseThrow().getStatus());
    PreRegistrationCouponRecipient recipient =
        recipientRepository.findByCouponId(coupon.getId()).orElseThrow();
    assertEquals(userId, recipient.getUserId());
    assertEquals(response.captureId(), recipient.getCaptureId());
    assertNull(recipient.getConvertedAt());
    assertEquals(initialCoins, userRepository.findById(userId).orElseThrow().getCoins());
  }

  @Test
  void returnsSameCardWhenSameUserRedeemsAgain() {
    persistCouponWithRecipient("TEST-RETRY", Instant.now().plus(Duration.ofDays(1)));

    CouponRedeemResponse first = couponRedeemService.redeem(userId, "TEST-RETRY");
    CouponRedeemResponse second = couponRedeemService.redeem(userId, "TEST-RETRY");

    assertEquals(first.captureId(), second.captureId());
  }

  @Test
  void rejectsRedeemWhenAnotherUserAlreadyMigrated() {
    persistCouponWithRecipient("TEST-TAKEN", Instant.now().plus(Duration.ofDays(1)));
    couponRedeemService.redeem(userId, "TEST-TAKEN");
    Long otherUserId = persistUser().getId();

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> couponRedeemService.redeem(otherUserId, "TEST-TAKEN"));

    assertEquals(ErrorCode.COUPON_ALREADY_USED.getCode(), exception.getCode());
  }

  @Test
  void rejectsUnknownCouponCode() {
    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> couponRedeemService.redeem(userId, "TEST-UNKNOWN"));

    assertEquals(ErrorCode.COUPON_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void rejectsCouponWithoutRecipient() {
    couponRepository.save(Coupon.issue("TEST-ORPHAN", Instant.now().plus(Duration.ofDays(1))));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> couponRedeemService.redeem(userId, "TEST-ORPHAN"));

    assertEquals(ErrorCode.COUPON_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void rejectsExpiredCoupon() {
    persistCouponWithRecipient("TEST-EXPIRED", Instant.now().minus(Duration.ofSeconds(1)));

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> couponRedeemService.redeem(userId, "TEST-EXPIRED"));

    assertEquals(ErrorCode.COUPON_EXPIRED.getCode(), exception.getCode());
  }

  @Test
  void rejectsSecondCouponForSameUser() {
    persistCouponWithRecipient("TEST-FIRST", Instant.now().plus(Duration.ofDays(1)));
    persistCouponWithRecipient("TEST-SECOND", Instant.now().plus(Duration.ofDays(1)));
    couponRedeemService.redeem(userId, "TEST-FIRST");

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> couponRedeemService.redeem(userId, "TEST-SECOND"));

    assertEquals(ErrorCode.COUPON_ALREADY_REDEEMED.getCode(), exception.getCode());
  }

  @Test
  void completesCouponWithAnimalImageAndGrantsCoins() {
    Coupon coupon =
        persistCouponWithRecipient("TEST-COMPLETE", Instant.now().plus(Duration.ofDays(1)));
    CouponRedeemResponse redeemed = couponRedeemService.redeem(userId, "TEST-COMPLETE");
    String animalImageKey = redeemed.animalImageUpload().key();
    simulateUpload(animalImageKey);

    CouponCompleteResponse response =
        couponCompleteService.complete(userId, "TEST-COMPLETE", animalImageKey);

    assertEquals(3000L, response.grantedCoins());
    assertEquals(initialCoins + 3000L, response.coins());
    assertEquals(initialCoins + 3000L, userRepository.findById(userId).orElseThrow().getCoins());

    Capture capture = captureRepository.findById(redeemed.captureId()).orElseThrow();
    assertEquals(GenerationStatus.SUCCEEDED, capture.getGenerationStatus());
    assertEquals(GameStatus.SUCCEEDED, capture.getGameStatus());
    assertEquals(animalImageKey, capture.getAnimalImage());

    assertEquals(
        CouponStatus.USED, couponRepository.findById(coupon.getId()).orElseThrow().getStatus());
    assertNotNull(
        recipientRepository.findByCouponId(coupon.getId()).orElseThrow().getConvertedAt());
  }

  @Test
  void rejectsCompleteBeforeRedeem() {
    persistCouponWithRecipient("TEST-NOT-STARTED", Instant.now().plus(Duration.ofDays(1)));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> couponCompleteService.complete(userId, "TEST-NOT-STARTED", "images/x/1/a.png"));

    assertEquals(ErrorCode.COUPON_REDEEM_NOT_STARTED.getCode(), exception.getCode());
  }

  @Test
  void rejectsCompleteByAnotherUser() {
    persistCouponWithRecipient("TEST-OTHERS", Instant.now().plus(Duration.ofDays(1)));
    CouponRedeemResponse redeemed = couponRedeemService.redeem(userId, "TEST-OTHERS");
    Long otherUserId = persistUser().getId();

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                couponCompleteService.complete(
                    otherUserId, "TEST-OTHERS", redeemed.animalImageUpload().key()));

    assertEquals(ErrorCode.FORBIDDEN_COUPON_ACCESS.getCode(), exception.getCode());
  }

  @Test
  void rejectsSecondComplete() {
    persistCouponWithRecipient("TEST-TWICE", Instant.now().plus(Duration.ofDays(1)));
    CouponRedeemResponse redeemed = couponRedeemService.redeem(userId, "TEST-TWICE");
    String animalImageKey = redeemed.animalImageUpload().key();
    simulateUpload(animalImageKey);
    couponCompleteService.complete(userId, "TEST-TWICE", animalImageKey);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> couponCompleteService.complete(userId, "TEST-TWICE", animalImageKey));

    assertEquals(ErrorCode.COUPON_ALREADY_USED.getCode(), exception.getCode());
    assertEquals(initialCoins + 3000L, userRepository.findById(userId).orElseThrow().getCoins());
  }

  @Test
  void rejectsCompleteWhenAnimalImageIsNotUploaded() {
    persistCouponWithRecipient("TEST-NO-UPLOAD", Instant.now().plus(Duration.ofDays(1)));
    CouponRedeemResponse redeemed = couponRedeemService.redeem(userId, "TEST-NO-UPLOAD");

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                couponCompleteService.complete(
                    userId, "TEST-NO-UPLOAD", redeemed.animalImageUpload().key()));

    assertEquals(ErrorCode.FILE_NOT_FOUND.getCode(), exception.getCode());
    assertEquals(initialCoins, userRepository.findById(userId).orElseThrow().getCoins());
  }

  private User persistUser() {
    return userRepository.save(
        User.register(
            SocialProvider.KAKAO, UUID.randomUUID().toString(), UUID.randomUUID() + "@t"));
  }

  private void simulateUpload(String key) {
    ((InMemoryFileStorage) fileStorage).put(key, 1024L, "image/png");
  }

  private Coupon persistCouponWithRecipient(String couponCode, Instant expiresAt) {
    Characterization characterization = persistSucceededCharacterization();
    PreRegistration preRegistration =
        preRegistrationRepository.save(
            PreRegistration.create(
                "encrypted-" + UUID.randomUUID(),
                "hash-" + UUID.randomUUID(),
                true,
                characterization.getId()));
    Coupon coupon = couponRepository.save(Coupon.issue(couponCode, expiresAt));
    recipientRepository.save(
        PreRegistrationCouponRecipient.issue(coupon.getId(), preRegistration.getId()));
    return coupon;
  }

  private Characterization persistSucceededCharacterization() {
    Characterization characterization =
        Characterization.start(
            1L,
            AnimalName.from("두부"),
            new CardMetadata(
                CardType.GROUND,
                50,
                CardSkill.GROUND_PAW_STRIKE,
                CardSkill.GROUND_LEAF_GUARD,
                "123"));
    characterization.succeed("characterizations/result.png", "test-provider", 100);
    return characterizationRepository.save(characterization);
  }

  private void cleanUp() {
    jdbcTemplate.update("delete from pre_registration_coupon_recipients");
    jdbcTemplate.update("delete from coupons");
  }
}
