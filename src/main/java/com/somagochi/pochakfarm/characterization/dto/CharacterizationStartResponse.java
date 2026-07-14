package com.somagochi.pochakfarm.characterization.dto;

import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record CharacterizationStartResponse(
    @Schema(description = "생성된 캐릭터라이징 ID", example = "1") Long characterizationId,
    @Schema(description = "캐릭터라이징 작업 상태", example = "PROCESSING") CharacterizationStatus status,
    @Schema(description = "카드 뒷면 리소스 매칭용 카드 타입", example = "GROUND") CardType cardType) {}
