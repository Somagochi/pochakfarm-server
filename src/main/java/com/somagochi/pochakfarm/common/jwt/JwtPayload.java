package com.somagochi.pochakfarm.common.jwt;

import java.time.Instant;
import java.util.Map;

public record JwtPayload(
    String tokenId,
    String subject,
    Instant issuedAt,
    Instant expiresAt,
    Map<String, Object> claims) {}
