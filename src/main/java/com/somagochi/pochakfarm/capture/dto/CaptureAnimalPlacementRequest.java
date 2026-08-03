package com.somagochi.pochakfarm.capture.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record CaptureAnimalPlacementRequest(
    @Schema(example = "images/capture-animal/1/123.png") String animalImageKey,
    @Schema(example = "1") Integer floorNum,
    @Schema(example = "2") Integer slotNum,
    @Schema(description = "교체할 현재 슬롯의 Animal ID. 빈 슬롯 저장 시 생략", example = "99", nullable = true)
        Long replacedAnimalId) {}
