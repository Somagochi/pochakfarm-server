package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.TermsAgreementRequest;
import com.somagochi.pochakfarm.user.dto.TermsAgreementResponse;
import com.somagochi.pochakfarm.user.dto.TermsAgreementUpdateRequest;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class UserTermsAgreementServiceTest {

  private static final Instant NOW = Instant.parse("2026-08-04T01:02:03Z");

  private final UserRepository userRepository = Mockito.mock(UserRepository.class);
  private final UserTermsAgreementService service =
      new UserTermsAgreementService(userRepository, Clock.fixed(NOW, ZoneOffset.UTC));

  @Test
  void recordsTermsAgreementForCurrentUser() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com", "포착이");
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));

    service.agree(1L, new TermsAgreementRequest(true, true, true, false, true));

    assertEquals(NOW, user.getRequiredTermsAgreedAt());
    assertEquals(NOW, user.getMarketingAgreedAt());
  }

  @Test
  void throwsWhenUserDoesNotExist() {
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.agree(1L, new TermsAgreementRequest(true, true, true, false, false)));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void returnsCurrentTermsAgreement() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com", "포착이");
    user.agreeToTerms(true, true, true, false, true, NOW);
    given(userRepository.findById(1L)).willReturn(Optional.of(user));

    TermsAgreementResponse response = service.get(1L);

    assertTrue(response.requiredTermsAgreed());
    assertEquals(NOW, response.requiredTermsAgreedAt());
    assertFalse(response.serviceQualityAgreed());
    assertNull(response.serviceQualityAgreedAt());
    assertTrue(response.marketingAgreed());
    assertEquals(NOW, response.marketingAgreedAt());
  }

  @Test
  void updatesMarketingAgreementAndReturnsCurrentTermsAgreement() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com", "포착이");
    user.agreeToTerms(true, true, true, false, false, NOW.minusSeconds(60));
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));

    TermsAgreementResponse response = service.update(1L, new TermsAgreementUpdateRequest(true));

    assertTrue(response.marketingAgreed());
    assertEquals(NOW, response.marketingAgreedAt());
  }

  @Test
  void rejectsNullMarketingAgreementUpdate() {
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com", "포착이");
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.of(user));

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.update(1L, new TermsAgreementUpdateRequest(null)));

    assertEquals(ErrorCode.INVALID_PARAMETER.getCode(), exception.getCode());
  }

  @Test
  void throwsWhenGettingTermsAgreementForMissingUser() {
    given(userRepository.findById(1L)).willReturn(Optional.empty());

    BusinessException exception = assertThrows(BusinessException.class, () -> service.get(1L));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }

  @Test
  void throwsWhenUpdatingTermsAgreementForMissingUser() {
    given(userRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.update(1L, new TermsAgreementUpdateRequest(true)));

    assertEquals(ErrorCode.USER_NOT_FOUND.getCode(), exception.getCode());
  }
}
