package com.somagochi.pochakfarm.coupon.application;

import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationQueryService;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@Slf4j
@RequiredArgsConstructor
public class PreRegistrationCouponService {

  private static final Instant COUPON_EXPIRES_AT = Instant.parse("2026-12-31T14:59:59Z");

  private final PreRegistrationQueryService preRegistrationQueryService;
  private final CouponIssueService couponIssueService;
  private final PreRegistrationCouponRecipientRepository recipientRepository;
  private final TransactionTemplate transactionTemplate;

  public int issue() {
    List<PreRegistration> targets = preRegistrationQueryService.findAllRegistered();
    int issued = 0;
    for (PreRegistration preRegistration : targets) {
      if (issueTo(preRegistration.getId())) {
        issued++;
      }
    }
    return issued;
  }

  private boolean issueTo(Long preRegistrationId) {
    try {
      return Boolean.TRUE.equals(
          transactionTemplate.execute(
              status -> {
                if (recipientRepository.existsByPreRegistrationId(preRegistrationId)) {
                  return false;
                }
                Coupon coupon = couponIssueService.issue(COUPON_EXPIRES_AT);
                recipientRepository.save(
                    PreRegistrationCouponRecipient.issue(coupon.getId(), preRegistrationId));
                return true;
              }));
    } catch (DataIntegrityViolationException e) {
      log.warn("pre_registration_coupon_issue_skipped preRegistrationId={}", preRegistrationId, e);
      return false;
    }
  }
}
