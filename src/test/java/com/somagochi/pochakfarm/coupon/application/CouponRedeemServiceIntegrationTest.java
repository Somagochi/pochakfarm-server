package com.somagochi.pochakfarm.coupon.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.animal.domain.Animal;
import com.somagochi.pochakfarm.animal.infrastructure.persistence.AnimalRepository;
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
import com.somagochi.pochakfarm.farm.application.FarmInitializationService;
import com.somagochi.pochakfarm.farm.domain.FarmSpace;
import com.somagochi.pochakfarm.farm.infrastructure.persistence.FarmSpaceRepository;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import com.somagochi.pochakfarm.storage.domain.FileStorage;
import com.somagochi.pochakfarm.storage.infrastructure.InMemoryFileStorage;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
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

  private static final String SOURCE_ANIMAL_IMAGE_KEY_FORMAT =
      "public/characterization-result/animal/%d.png";
  private static final String ANIMAL_IMAGE_KEY_FORMAT = "public/capture-animal/%d/%d.png";

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
  @Autowired private AnimalRepository animalRepository;
  @Autowired private FarmSpaceRepository farmSpaceRepository;
  @Autowired private FarmInitializationService farmInitializationService;
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

    CouponCompleteResponse response = couponCompleteService.complete(userId, "TEST-COMPLETE");

    assertEquals(3000L, response.grantedCoins());
    assertEquals(initialCoins + 3000L, response.coins());
    assertEquals(initialCoins + 3000L, userRepository.findById(userId).orElseThrow().getCoins());

    Capture capture = captureRepository.findById(redeemed.captureId()).orElseThrow();
    assertEquals(GenerationStatus.SUCCEEDED, capture.getGenerationStatus());
    assertEquals(GameStatus.SUCCEEDED, capture.getGameStatus());
    String expectedAnimalImageKey = ANIMAL_IMAGE_KEY_FORMAT.formatted(userId, redeemed.captureId());
    assertEquals(expectedAnimalImageKey, capture.getAnimalImage());
    assertNotNull(fileStorage.head(expectedAnimalImageKey));

    assertEquals(
        CouponStatus.USED, couponRepository.findById(coupon.getId()).orElseThrow().getStatus());
    assertNotNull(
        recipientRepository.findByCouponId(coupon.getId()).orElseThrow().getConvertedAt());

    FarmSpace space =
        farmSpaceRepository.findByUserIdAndType(userId, CardType.GROUND).orElseThrow();
    List<Animal> animals =
        animalRepository.findBySpaceIdAndFloorNumBetween(
            space.getId(), FarmSpace.FIRST_FLOOR, space.getFloor());
    assertEquals(1, animals.size());
    assertEquals(redeemed.captureId(), animals.get(0).getCaptureId());
    assertEquals(FarmSpace.FIRST_FLOOR, animals.get(0).getFloorNum());
    assertEquals(FarmSpace.FIRST_SLOT, animals.get(0).getSlotNum());
  }

  @Test
  void rejectsRedeemWhenFarmSpaceIsFull() {
    persistCouponWithRecipient("TEST-FARM-FULL", Instant.now().plus(Duration.ofDays(1)));
    fillGroundFarm(userId);

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> couponRedeemService.redeem(userId, "TEST-FARM-FULL"));

    assertEquals(ErrorCode.FARM_SPACE_FULL.getCode(), exception.getCode());
  }

  @Test
  void rejectsCompleteWhenFarmSpaceBecameFull() {
    Coupon coupon =
        persistCouponWithRecipient("TEST-FULL-LATE", Instant.now().plus(Duration.ofDays(1)));
    couponRedeemService.redeem(userId, "TEST-FULL-LATE");
    fillGroundFarm(userId);

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> couponCompleteService.complete(userId, "TEST-FULL-LATE"));

    assertEquals(ErrorCode.FARM_SPACE_FULL.getCode(), exception.getCode());
    assertEquals(initialCoins, userRepository.findById(userId).orElseThrow().getCoins());
    assertEquals(
        CouponStatus.ACTIVE, couponRepository.findById(coupon.getId()).orElseThrow().getStatus());
  }

  @Test
  void rejectsCompleteBeforeRedeem() {
    persistCouponWithRecipient("TEST-NOT-STARTED", Instant.now().plus(Duration.ofDays(1)));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> couponCompleteService.complete(userId, "TEST-NOT-STARTED"));

    assertEquals(ErrorCode.COUPON_REDEEM_NOT_STARTED.getCode(), exception.getCode());
  }

  @Test
  void rejectsCompleteByAnotherUser() {
    persistCouponWithRecipient("TEST-OTHERS", Instant.now().plus(Duration.ofDays(1)));
    couponRedeemService.redeem(userId, "TEST-OTHERS");
    Long otherUserId = persistUser().getId();

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> couponCompleteService.complete(otherUserId, "TEST-OTHERS"));

    assertEquals(ErrorCode.FORBIDDEN_COUPON_ACCESS.getCode(), exception.getCode());
  }

  @Test
  void rejectsSecondComplete() {
    persistCouponWithRecipient("TEST-TWICE", Instant.now().plus(Duration.ofDays(1)));
    couponRedeemService.redeem(userId, "TEST-TWICE");
    couponCompleteService.complete(userId, "TEST-TWICE");

    BusinessException exception =
        assertThrows(
            BusinessException.class, () -> couponCompleteService.complete(userId, "TEST-TWICE"));

    assertEquals(ErrorCode.COUPON_ALREADY_USED.getCode(), exception.getCode());
    assertEquals(initialCoins + 3000L, userRepository.findById(userId).orElseThrow().getCoins());
  }

  @Test
  void rejectsCompleteWhenAnimalImageSourceIsMissing() {
    persistCouponWithRecipient("TEST-NO-SOURCE", Instant.now().plus(Duration.ofDays(1)), false);
    couponRedeemService.redeem(userId, "TEST-NO-SOURCE");

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> couponCompleteService.complete(userId, "TEST-NO-SOURCE"));

    assertEquals(ErrorCode.FILE_NOT_FOUND.getCode(), exception.getCode());
    assertEquals(initialCoins, userRepository.findById(userId).orElseThrow().getCoins());
  }

  private User persistUser() {
    User user =
        userRepository.save(
            User.register(
                SocialProvider.KAKAO,
                UUID.randomUUID().toString(),
                UUID.randomUUID() + "@t",
                "u" + UUID.randomUUID().toString().substring(0, 5)));
    farmInitializationService.initialize(user.getId());
    return user;
  }

  private void fillGroundFarm(Long userId) {
    FarmSpace space =
        farmSpaceRepository.findByUserIdAndType(userId, CardType.GROUND).orElseThrow();
    for (int floorNum = FarmSpace.FIRST_FLOOR; floorNum <= space.getFloor(); floorNum++) {
      for (int slotNum = FarmSpace.FIRST_SLOT;
          slotNum <= FarmSpace.SLOT_COUNT_PER_FLOOR;
          slotNum++) {
        Capture capture = captureRepository.save(occupyingCapture(userId));
        animalRepository.save(Animal.create(capture.getId(), space.getId(), floorNum, slotNum));
      }
    }
  }

  private Capture occupyingCapture(Long userId) {
    return Capture.granted(
        userId,
        CardType.GROUND,
        Tier.S,
        AnimalName.from("점유"),
        CardSkill.GROUND_PAW_STRIKE,
        CardSkill.GROUND_LEAF_GUARD,
        "999",
        "captures/card.png",
        "image/png",
        Instant.now());
  }

  private Coupon persistCouponWithRecipient(String couponCode, Instant expiresAt) {
    return persistCouponWithRecipient(couponCode, expiresAt, true);
  }

  private Coupon persistCouponWithRecipient(
      String couponCode, Instant expiresAt, boolean sourceImageUploaded) {
    Characterization characterization = persistSucceededCharacterization(sourceImageUploaded);
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

  private Characterization persistSucceededCharacterization(boolean sourceImageUploaded) {
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
    Characterization saved = characterizationRepository.save(characterization);
    String sourceKey = SOURCE_ANIMAL_IMAGE_KEY_FORMAT.formatted(saved.getId());
    jdbcTemplate.update(
        "update characterizations set animal_image_key = ? where id = ?", sourceKey, saved.getId());
    if (sourceImageUploaded) {
      ((InMemoryFileStorage) fileStorage).put(sourceKey, 1024L, "image/png");
    }
    return saved;
  }

  private void cleanUp() {
    jdbcTemplate.update("delete from pre_registration_coupon_recipients");
    jdbcTemplate.update("delete from coupons");
  }
}
