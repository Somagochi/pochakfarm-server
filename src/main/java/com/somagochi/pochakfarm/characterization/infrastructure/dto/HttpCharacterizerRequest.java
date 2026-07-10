package com.somagochi.pochakfarm.characterization.infrastructure.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HttpCharacterizerRequest(
    @JsonProperty("source_image_base64") String sourceImageBase64,
    @JsonProperty("source_image_content_type") String sourceImageContentType,
    @JsonProperty("animal_name") String animalName,
    @JsonProperty("card_type") String cardType,
    @JsonProperty("card_type_label") String cardTypeLabel,
    Integer power,
    @JsonProperty("skill_1_name") String skill1Name,
    @JsonProperty("skill_1_description") String skill1Description,
    @JsonProperty("skill_2_name") String skill2Name,
    @JsonProperty("skill_2_description") String skill2Description,
    @JsonProperty("card_no") String cardNo) {}
