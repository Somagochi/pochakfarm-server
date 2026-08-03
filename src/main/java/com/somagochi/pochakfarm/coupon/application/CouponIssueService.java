package com.somagochi.pochakfarm.coupon.application;

import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.CouponRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponIssueService {

  private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  private static final int CODE_LENGTH = 6;
  private static final int MAX_CODE_ATTEMPTS = 5;

  private final CouponRepository couponRepository;
  private final CouponCodeGenerator couponCodeGenerator;

  public Coupon issue(Instant expiresAt) {
    return couponRepository.save(Coupon.issue(generateUniqueCode(), expiresAt));
  }

  private String generateUniqueCode() {
    for (int attempt = 0; attempt < MAX_CODE_ATTEMPTS; attempt++) {
      String code = couponCodeGenerator.generate(CODE_ALPHABET, CODE_LENGTH);
      if (!couponRepository.existsByCouponCode(code)) {
        return code;
      }
    }
    throw new IllegalStateException("Coupon code generation attempts exhausted");
  }
}
