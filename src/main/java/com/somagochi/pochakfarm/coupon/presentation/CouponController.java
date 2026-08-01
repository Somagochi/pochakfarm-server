package com.somagochi.pochakfarm.coupon.presentation;

import com.somagochi.pochakfarm.common.response.ApiResponse;
import com.somagochi.pochakfarm.common.security.UserPrincipal;
import com.somagochi.pochakfarm.coupon.application.CouponCompleteService;
import com.somagochi.pochakfarm.coupon.application.CouponRedeemService;
import com.somagochi.pochakfarm.coupon.dto.CouponCompleteRequest;
import com.somagochi.pochakfarm.coupon.dto.CouponCompleteResponse;
import com.somagochi.pochakfarm.coupon.dto.CouponRedeemRequest;
import com.somagochi.pochakfarm.coupon.dto.CouponRedeemResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController implements CouponApiSpec {

  private final CouponRedeemService couponRedeemService;
  private final CouponCompleteService couponCompleteService;

  @Override
  @PostMapping(path = "/redeem")
  public ApiResponse<CouponRedeemResponse> redeem(
      @AuthenticationPrincipal UserPrincipal principal, @RequestBody CouponRedeemRequest request) {
    return ApiResponse.success(couponRedeemService.redeem(principal.id(), request.couponCode()));
  }

  @Override
  @PostMapping(path = "/complete")
  public ApiResponse<CouponCompleteResponse> complete(
      @AuthenticationPrincipal UserPrincipal principal,
      @RequestBody CouponCompleteRequest request) {
    return ApiResponse.success(
        couponCompleteService.complete(
            principal.id(), request.couponCode(), request.animalImageKey()));
  }
}
