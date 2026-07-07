package com.somagochi.pochakfarm.storage.dto;

import java.time.Instant;

public record PresignResponse(String uploadUrl, String key, Instant expiresAt) {}
