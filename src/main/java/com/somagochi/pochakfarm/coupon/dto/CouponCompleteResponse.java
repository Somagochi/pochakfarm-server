package com.somagochi.pochakfarm.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CouponCompleteResponse(
    @Schema(description = "지급된 코인 수량", example = "3000") long grantedCoins,
    @Schema(description = "지급 후 보유 코인", example = "4000") long coins) {}
