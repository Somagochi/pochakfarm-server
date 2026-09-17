package com.somagochi.pochakfarm.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "뱃지 코드 커서 기반 페이지")
public record OwnedBadgePage(
    @Schema(description = "현재 페이지 항목") List<OwnedBadgeResponse> content,
    @Schema(description = "다음 페이지 커서(마지막 항목의 뱃지 코드). 없으면 null", example = "BDG020")
        String nextCursor,
    @Schema(description = "다음 페이지 존재 여부", example = "true") boolean hasNext) {}
