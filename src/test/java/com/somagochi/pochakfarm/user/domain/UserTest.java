package com.somagochi.pochakfarm.user.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class UserTest {

  @Test
  void registerStartsAtLevelOneWithNoExperienceAndInitialCoins() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    assertEquals(1, user.getLevel());
    assertEquals(0, user.getExperience());
    assertEquals(1000, user.getCoins());
    assertTrue(user.isTermsAgreementRequired());
  }

  @Test
  void agreesToRequiredAndSelectedOptionalTerms() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    Instant agreedAt = Instant.parse("2026-08-04T01:02:03Z");

    user.agreeToTerms(true, true, true, false, true, agreedAt);

    assertEquals(agreedAt, user.getRequiredTermsAgreedAt());
    assertNull(user.getServiceQualityAgreedAt());
    assertEquals(agreedAt, user.getMarketingAgreedAt());
    assertFalse(user.isTermsAgreementRequired());
  }

  @Test
  void rejectsTermsAgreementWhenAnyRequiredConsentIsMissing() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                user.agreeToTerms(
                    true, false, true, true, true, Instant.parse("2026-08-04T01:02:03Z")));

    assertEquals(ErrorCode.REQUIRED_CONSENT_REQUIRED.getCode(), exception.getCode());
    assertNull(user.getRequiredTermsAgreedAt());
    assertNull(user.getServiceQualityAgreedAt());
    assertNull(user.getMarketingAgreedAt());
  }

  @Test
  void rejectsTermsAgreementWhenRequiredConsentIsNull() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                user.agreeToTerms(
                    true, true, null, false, false, Instant.parse("2026-08-04T01:02:03Z")));

    assertEquals(ErrorCode.REQUIRED_CONSENT_REQUIRED.getCode(), exception.getCode());
  }

  @Test
  void termsAgreementIsIdempotent() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    Instant firstAgreedAt = Instant.parse("2026-08-04T01:02:03Z");
    user.agreeToTerms(true, true, true, false, true, firstAgreedAt);

    user.agreeToTerms(true, true, true, true, false, Instant.parse("2026-08-05T01:02:03Z"));

    assertEquals(firstAgreedAt, user.getRequiredTermsAgreedAt());
    assertNull(user.getServiceQualityAgreedAt());
    assertEquals(firstAgreedAt, user.getMarketingAgreedAt());
  }

  @Test
  void agreesToMarketingWhenCurrentlyNotAgreed() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    Instant initialAgreedAt = Instant.parse("2026-08-04T01:02:03Z");
    Instant marketingAgreedAt = Instant.parse("2026-08-05T01:02:03Z");
    user.agreeToTerms(true, true, true, false, false, initialAgreedAt);

    user.updateMarketingAgreement(true, marketingAgreedAt);

    assertEquals(marketingAgreedAt, user.getMarketingAgreedAt());
  }

  @Test
  void keepsOriginalMarketingAgreementTimeWhenAlreadyAgreed() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    Instant firstAgreedAt = Instant.parse("2026-08-04T01:02:03Z");
    user.agreeToTerms(true, true, true, false, true, firstAgreedAt);

    user.updateMarketingAgreement(true, Instant.parse("2026-08-05T01:02:03Z"));

    assertEquals(firstAgreedAt, user.getMarketingAgreedAt());
  }

  @Test
  void withdrawsMarketingAgreement() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    user.agreeToTerms(true, true, true, false, true, Instant.parse("2026-08-04T01:02:03Z"));

    user.updateMarketingAgreement(false, Instant.parse("2026-08-05T01:02:03Z"));

    assertNull(user.getMarketingAgreedAt());
  }

  @Test
  void keepsMarketingAgreementEmptyWhenAlreadyNotAgreed() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    user.agreeToTerms(true, true, true, false, false, Instant.parse("2026-08-04T01:02:03Z"));

    user.updateMarketingAgreement(false, Instant.parse("2026-08-05T01:02:03Z"));

    assertNull(user.getMarketingAgreedAt());
  }

  @Test
  void rejectsNullMarketingAgreement() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> user.updateMarketingAgreement(null, Instant.parse("2026-08-05T01:02:03Z")));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
  }

  @Test
  void changeNicknameUpdatesTrimmedNickname() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    user.changeNickname("  포착이  ");

    assertEquals("포착이", user.getNickname());
  }

  @Test
  void changeNicknameRejectsBlankNickname() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> user.changeNickname("   "));

    assertEquals(ErrorCode.INVALID_NICKNAME.getCode(), exception.getCode());
  }

  @Test
  void changeNicknameRejectsNullNickname() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> user.changeNickname(null));

    assertEquals(ErrorCode.INVALID_NICKNAME.getCode(), exception.getCode());
  }

  @Test
  void changeNicknameRejectsTooLongNickname() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> user.changeNickname("a".repeat(21)));

    assertEquals(ErrorCode.INVALID_NICKNAME.getCode(), exception.getCode());
  }

  @Test
  void gainsExperienceAndAppliesLevelReward() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    LevelReward reward = user.gainExperience(40, new LevelRewardPolicy());

    assertEquals(40, reward.experienceReward());
    assertEquals(500, reward.coinReward());
    assertEquals(2, user.getLevel());
    assertEquals(0, user.getExperience());
    assertEquals(1000, user.getCoins());
  }

  @Test
  void spendsCoins() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    user.spendCoins(200);

    assertEquals(800, user.getCoins());
  }

  @Test
  void rejectsSpendingCoinsWhenInsufficient() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    BusinessException exception =
        assertThrows(BusinessException.class, () -> user.spendCoins(1200));

    assertEquals(ErrorCode.INSUFFICIENT_COINS.getCode(), exception.getCode());
    assertEquals(1000, user.getCoins());
  }

  @Test
  void withdrawMarksDeletedAndAnonymizesUniqueIdentifiers() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");
    user.changeNickname("포착이");

    user.withdraw(WithdrawalReason.INCONVENIENT);

    assertTrue(user.isDeleted());
    assertEquals(WithdrawalReason.INCONVENIENT, user.getWithdrawalReason());
    assertNull(user.getNickname());
    assertEquals(SocialProvider.KAKAO, user.getSocialAccount().getProvider());
    assertNotEquals("provider-id-1", user.getSocialAccount().getProviderId());
    assertTrue(user.getSocialAccount().getProviderId().startsWith("deleted-"));
    assertTrue(user.getSocialAccount().getProviderId().endsWith("-provider-id-1"));
    assertTrue(user.getEmail().startsWith("deleted-"));
    assertTrue(user.getEmail().endsWith("-test123@test.com"));
  }

  @Test
  void withdrawIsIdempotent() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", "test123@test.com");

    user.withdraw(WithdrawalReason.LOW_USAGE);
    String providerIdAfterFirst = user.getSocialAccount().getProviderId();
    String emailAfterFirst = user.getEmail();

    user.withdraw(WithdrawalReason.OTHER);

    assertEquals(providerIdAfterFirst, user.getSocialAccount().getProviderId());
    assertEquals(emailAfterFirst, user.getEmail());
    assertEquals(WithdrawalReason.LOW_USAGE, user.getWithdrawalReason());
  }

  @Test
  void withdrawHandlesNullEmail() {
    User user = User.register(SocialProvider.KAKAO, "provider-id-1", null);

    user.withdraw(null);

    assertTrue(user.isDeleted());
    assertNull(user.getWithdrawalReason());
    assertFalse(user.getSocialAccount().getProviderId().equals("provider-id-1"));
  }
}
