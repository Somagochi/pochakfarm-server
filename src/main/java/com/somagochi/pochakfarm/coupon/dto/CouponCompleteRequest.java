package com.somagochi.pochakfarm.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CouponCompleteRequest(
    @Schema(description = "사전예약 쿠폰 코드", example = "POCHAK-A1B2C3D4") String couponCode) {}
