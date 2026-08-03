package com.somagochi.pochakfarm.coupon.application;

import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponQueryService {

  private final PreRegistrationCouponRecipientRepository recipientRepository;

  @Transactional(readOnly = true)
  public boolean hasConvertedPreRegistrationCoupon(Long userId) {
    return recipientRepository.existsByUserIdAndConvertedAtIsNotNull(userId);
  }
}
