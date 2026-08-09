package com.somagochi.pochakfarm.preregistration.dto;

public record PreRegistrationCouponSmsResult(
    int targetCount, int sentCount, int failedCount, int couponNotIssuedCount, boolean dryRun) {}
