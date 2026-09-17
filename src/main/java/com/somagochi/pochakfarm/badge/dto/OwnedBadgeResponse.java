package com.somagochi.pochakfarm.badge.dto;

import com.somagochi.pochakfarm.badge.domain.BadgeCategory;
import java.time.Instant;

public record OwnedBadgeResponse(
    String code,
    BadgeCategory category,
    String name,
    String description,
    String imageUrl,
    Instant acquiredAt) {}
