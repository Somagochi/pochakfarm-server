package com.somagochi.pochakfarm.user.dto;

import com.somagochi.pochakfarm.user.domain.WithdrawalReason;
import io.swagger.v3.oas.annotations.media.Schema;

public record WithdrawRequest(
    @Schema(description = "함께 무효화할 리프레시 토큰", example = "eyJhbGciOiJIUzI1NiJ9.xxxxx.yyyyy")
        String refreshToken,
    @Schema(description = "회원 탈퇴 사유. 선택하지 않으면 생략할 수 있다.", nullable = true, example = "LOW_USAGE")
        WithdrawalReason withdrawalReason) {}
