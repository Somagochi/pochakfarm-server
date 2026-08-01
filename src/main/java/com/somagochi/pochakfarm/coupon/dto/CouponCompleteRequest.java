package com.somagochi.pochakfarm.coupon.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CouponCompleteRequest(
    @Schema(description = "사전예약 쿠폰 코드", example = "POCHAK-A1B2C3D4") String couponCode,
    @Schema(description = "클라이언트가 업로드한 누끼(동물) 이미지 키", example = "images/coupon-animal/1/uuid.png")
        String animalImageKey) {}
