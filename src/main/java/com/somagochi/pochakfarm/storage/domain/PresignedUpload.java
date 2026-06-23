package com.somagochi.pochakfarm.storage.domain;

import java.time.Instant;

public record PresignedUpload(String url, Instant expiresAt) {}
