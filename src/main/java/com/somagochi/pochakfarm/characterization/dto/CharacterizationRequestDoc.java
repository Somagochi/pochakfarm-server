package com.somagochi.pochakfarm.characterization.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

@Schema(name = "CharacterizationRequest", description = "이미지 변환 multipart/form-data 요청")
public record CharacterizationRequestDoc(
    @Schema(
            description = "변환할 원본 이미지 파일. 지원 형식: image/jpeg, image/png, image/webp",
            format = "binary")
        MultipartFile image,
    @Schema(description = "결과 이미지에 반영할 반려동물 이름. 공백 포함 최대 6글자", example = "솜구름", maxLength = 6)
        String animalName) {}
