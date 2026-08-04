package com.somagochi.pochakfarm.user.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.BDDMockito.given;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.common.social.SocialProvider;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.TermsAgreementRequest;
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
    User user = User.register(SocialProvider.KAKAO, "provider-id", "user@example.com");
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
}
