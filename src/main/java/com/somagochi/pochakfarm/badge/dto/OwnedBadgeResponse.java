package com.somagochi.pochakfarm.badge.dto;

import java.time.Instant;

public record OwnedBadgeResponse(
    String code, String name, String description, String imageUrl, Instant acquiredAt) {}
