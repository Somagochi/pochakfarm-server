package com.somagochi.pochakfarm.auth.dto;

public record SocialLoginResponse(TokenResponse token, Boolean isNew) {}
