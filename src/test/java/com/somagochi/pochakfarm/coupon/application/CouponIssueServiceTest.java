package com.somagochi.pochakfarm.coupon.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.somagochi.pochakfarm.coupon.domain.Coupon;
import com.somagochi.pochakfarm.coupon.infrastructure.persistence.CouponRepository;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CouponIssueServiceTest {

  private static final Instant EXPIRES_AT = Instant.parse("2026-12-31T14:59:59Z");

  @Mock private CouponRepository couponRepository;
  @Mock private CouponCodeGenerator couponCodeGenerator;

  private CouponIssueService service;

  @BeforeEach
  void setUp() {
    service = new CouponIssueService(couponRepository, couponCodeGenerator);
  }

  @Test
  void issuesCouponWithGeneratedCode() {
    given(couponCodeGenerator.generate(anyString(), anyInt())).willReturn("AAAAAA");
    given(couponRepository.existsByCouponCode("AAAAAA")).willReturn(false);
    given(couponRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    Coupon coupon = service.issue(EXPIRES_AT);

    assertEquals("AAAAAA", coupon.getCouponCode());
    assertEquals(EXPIRES_AT, coupon.getExpiresAt());
  }

  @Test
  void regeneratesCodeOnCollision() {
    given(couponCodeGenerator.generate(anyString(), anyInt())).willReturn("AAAAAA", "BBBBBB");
    given(couponRepository.existsByCouponCode("AAAAAA")).willReturn(true);
    given(couponRepository.existsByCouponCode("BBBBBB")).willReturn(false);
    given(couponRepository.save(any())).willAnswer(invocation -> invocation.getArgument(0));

    Coupon coupon = service.issue(EXPIRES_AT);

    assertEquals("BBBBBB", coupon.getCouponCode());
    ArgumentCaptor<Coupon> captor = ArgumentCaptor.forClass(Coupon.class);
    verify(couponRepository).save(captor.capture());
    assertEquals("BBBBBB", captor.getValue().getCouponCode());
  }

  @Test
  void throwsWhenCodeGenerationAttemptsExhausted() {
    given(couponCodeGenerator.generate(anyString(), anyInt())).willReturn("AAAAAA");
    given(couponRepository.existsByCouponCode("AAAAAA")).willReturn(true);

    assertThrows(IllegalStateException.class, () -> service.issue(EXPIRES_AT));
    verify(couponRepository, never()).save(any());
  }
}
