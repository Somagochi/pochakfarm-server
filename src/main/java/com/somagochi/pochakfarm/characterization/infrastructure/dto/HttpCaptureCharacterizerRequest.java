package com.somagochi.pochakfarm.characterization.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HttpCaptureCharacterizerRequest(
    @JsonProperty("original_image_download_url") String originalImageDownloadUrl,
    @JsonProperty("animal_image_upload_url") String animalImageUploadUrl,
    @JsonProperty("card_image_upload_url") String cardImageUploadUrl,
    @JsonProperty("animal_name") String animalName,
    @JsonProperty("card_type") String cardType,
    @JsonProperty("card_type_label") String cardTypeLabel,
    String tier,
    @JsonProperty("skill_1_name") String skill1Name,
    @JsonProperty("skill_1_description") String skill1Description,
    @JsonProperty("skill_2_name") String skill2Name,
    @JsonProperty("skill_2_description") String skill2Description,
    @JsonProperty("card_no") String cardNo) {}
