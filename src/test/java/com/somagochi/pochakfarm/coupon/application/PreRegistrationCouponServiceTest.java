package com.somagochi.pochakfarm.coupon.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.domain.PreRegistrationCouponRecipient;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.PreRegistrationCouponRecipientRepository;
import com.somagochi.pochakfarm.preregistration.application.PreRegistrationQueryService;
import com.somagochi.pochakfarm.preregistration.domain.PreRegistration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ExtendWith(MockitoExtension.class)
class PreRegistrationCouponServiceTest {

  @Mock private PreRegistrationQueryService preRegistrationQueryService;
  @Mock private CouponIssueService couponIssueService;
  @Mock private PreRegistrationCouponRecipientRepository recipientRepository;
  @Mock private PlatformTransactionManager transactionManager;

  private PreRegistrationCouponService service;

  @BeforeEach
  void setUp() {
    service =
        new PreRegistrationCouponService(
            preRegistrationQueryService,
            couponIssueService,
            recipientRepository,
            new TransactionTemplate(transactionManager));
  }

  @Test
  void issuesCouponAndRecipientForEachTarget() {
    given(preRegistrationQueryService.findAllRegistered())
        .willReturn(List.of(preRegistration(1L), preRegistration(2L)));
    given(recipientRepository.existsByPreRegistrationId(any())).willReturn(false);
    given(couponIssueService.issue(any())).willAnswer(invocation -> coupon(100L));
    given(recipientRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    int issued = service.issue();

    assertEquals(2, issued);
    ArgumentCaptor<PreRegistrationCouponRecipient> captor =
        ArgumentCaptor.forClass(PreRegistrationCouponRecipient.class);
    verify(recipientRepository, times(2)).save(captor.capture());
    assertEquals(1L, captor.getAllValues().get(0).getPreRegistrationId());
    assertEquals(100L, captor.getAllValues().get(0).getCouponId());
    assertEquals(2L, captor.getAllValues().get(1).getPreRegistrationId());
  }

  @Test
  void skipsPreRegistrationThatAlreadyHasCoupon() {
    given(preRegistrationQueryService.findAllRegistered()).willReturn(List.of(preRegistration(1L)));
    given(recipientRepository.existsByPreRegistrationId(1L)).willReturn(true);

    int issued = service.issue();

    assertEquals(0, issued);
    verify(couponIssueService, never()).issue(any());
    verify(recipientRepository, never()).save(any());
  }

  @Test
  void continuesRemainingTargetsWhenSaveViolatesUniqueConstraint() {
    given(preRegistrationQueryService.findAllRegistered())
        .willReturn(List.of(preRegistration(1L), preRegistration(2L)));
    given(recipientRepository.existsByPreRegistrationId(any())).willReturn(false);
    given(couponIssueService.issue(any())).willAnswer(invocation -> coupon(100L));
    given(recipientRepository.save(any()))
        .willThrow(new DataIntegrityViolationException("duplicate"))
        .willAnswer(invocation -> invocation.getArgument(0));

    int issued = service.issue();

    assertEquals(1, issued);
    verify(recipientRepository, times(2)).save(any());
  }

  private PreRegistration preRegistration(Long id) {
    PreRegistration preRegistration =
        PreRegistration.create("enc-" + UUID.randomUUID(), "hash-" + UUID.randomUUID(), true, 1L);
    ReflectionTestUtils.setField(preRegistration, "id", id);
    return preRegistration;
  }

  private Coupon coupon(Long id) {
    Coupon coupon = Coupon.issue("AAAAAA", Instant.parse("2026-12-31T14:59:59Z"));
    ReflectionTestUtils.setField(coupon, "id", id);
    return coupon;
  }
}
