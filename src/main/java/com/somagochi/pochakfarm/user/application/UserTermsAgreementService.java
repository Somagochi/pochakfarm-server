package com.somagochi.pochakfarm.user.application;

import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.somagochi.pochakfarm.user.domain.User;
import com.somagochi.pochakfarm.user.dto.TermsAgreementRequest;
import com.somagochi.pochakfarm.user.infrastructure.persistence.UserRepository;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserTermsAgreementService {

  private final UserRepository userRepository;
  private final Clock clock;

  @Transactional
  public void agree(Long userId, TermsAgreementRequest request) {
    User user =
        userRepository
            .findByIdForUpdate(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    user.agreeToTerms(
        request.ageRequirementAgreed(),
        request.termsOfServiceAgreed(),
        request.privacyPolicyAgreed(),
        request.serviceQualityAgreed(),
        request.marketingAgreed(),
        clock.instant());
  }
}
