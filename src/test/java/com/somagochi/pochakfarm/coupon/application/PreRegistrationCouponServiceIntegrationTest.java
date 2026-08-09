package com.somagochi.pochakfarm.coupon.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.domain.CouponStatus;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.CouponRepository;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import com.somagochi.pochakfarm.preregistration.infrastructure.persistence.PreRegistrationRepository;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class PreRegistrationCouponServiceIntegrationTest {

  private static final String CODE_PATTERN = "[ABCDEFGHJKLMNPQRSTUVWXYZ23456789]{6}";

  @Autowired private PreRegistrationCouponService preRegistrationCouponService;
  @Autowired private CouponRepository couponRepository;
  @Autowired private PreRegistrationCouponRecipientRepository recipientRepository;
  @Autowired private PreRegistrationRepository preRegistrationRepository;
  @Autowired private JdbcTemplate jdbcTemplate;

  @BeforeEach
  void setUp() {
    cleanUp();
  }

  @AfterEach
  void tearDown() {
    cleanUp();
  }

  @Test
  void issuesCouponAndRecipientForEachRegisteredPreRegistration() {
    PreRegistration first = persistPreRegistration();
    PreRegistration second = persistPreRegistration();

    int issued = preRegistrationCouponService.issue();

    assertEquals(2, issued);
    assertEquals(2, couponRepository.count());
    assertTrue(recipientRepository.existsByPreRegistrationId(first.getId()));
    assertTrue(recipientRepository.existsByPreRegistrationId(second.getId()));
    for (Coupon coupon : couponRepository.findAll()) {
      assertTrue(coupon.getCouponCode().matches(CODE_PATTERN));
      assertEquals(CouponStatus.ACTIVE, coupon.getStatus());
    }
  }

  @Test
  void rerunDoesNotIssueDuplicates() {
    persistPreRegistration();

    int first = preRegistrationCouponService.issue();
    int second = preRegistrationCouponService.issue();

    assertEquals(1, first);
    assertEquals(0, second);
    assertEquals(1, couponRepository.count());
    assertEquals(1, recipientRepository.count());
  }

  @Test
  void excludesCanceledPreRegistrations() {
    PreRegistration canceled = persistPreRegistration();
    canceled.cancel();
    preRegistrationRepository.save(canceled);

    int issued = preRegistrationCouponService.issue();

    assertEquals(0, issued);
    assertEquals(0, couponRepository.count());
    assertEquals(0, recipientRepository.count());
  }

  private PreRegistration persistPreRegistration() {
    return preRegistrationRepository.save(
        PreRegistration.create("enc-" + UUID.randomUUID(), "hash-" + UUID.randomUUID(), true, 1L));
  }

  private void cleanUp() {
    jdbcTemplate.update("delete from pre_registration_coupon_recipients");
    jdbcTemplate.update("delete from coupons");
    jdbcTemplate.update("delete from pre_registrations");
  }
}
