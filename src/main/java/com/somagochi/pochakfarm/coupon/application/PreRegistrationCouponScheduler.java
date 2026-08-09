package com.somagochi.pochakfarm.coupon.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.coupon.pre-registration",
    name = "enabled",
    havingValue = "true")
public class PreRegistrationCouponScheduler {

  private final PreRegistrationCouponService preRegistrationCouponService;

  @Scheduled(cron = "0 0 * * * *")
  public void issueCoupons() {
    int issued = preRegistrationCouponService.issue();
    if (issued > 0) {
      log.info("pre_registration_coupons_issued count={}", issued);
    }
  }
}
