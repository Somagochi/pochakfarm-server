package com.somagochi.pochakfarm.characterization.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@Slf4j
public class HttpCharacterizerClient implements CharacterizerClient {

  private final RestClient restClient;
  private final String baseUrl;

  public HttpCharacterizerClient(
      @Value("${app.characterizer.base-url:http://localhost:8000}") String baseUrl,
      @Value("${app.characterizer.connect-timeout:PT5S}") Duration connectTimeout,
      @Value("${app.characterizer.read-timeout:PT6M}") Duration readTimeout) {
    this.baseUrl = baseUrl;
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  @Override
  public CharacterizerResult characterize(
      String sourceImageUrl, String animalName, CardMetadata metadata) {
    long startedAt = System.nanoTime();
    try {
      log.info(
          "characterizer_request_started baseUrl={} sourceImageUrl={}", baseUrl, sourceImageUrl);
      HttpCharacterizationResponse response =
          restClient
              .post()
              .uri("/internal/characterize")
              .contentType(MediaType.APPLICATION_JSON)
              .body(createRequestBody(sourceImageUrl, animalName, metadata))
              .retrieve()
              .body(HttpCharacterizationResponse.class);
      if (response == null
          || !"success".equals(response.status())
          || response.aiImageBase64() == null
          || response.aiImageBase64().isBlank()
          || response.cardImageBase64() == null
          || response.cardImageBase64().isBlank()
          || response.cardBackImageBase64() == null
          || response.cardBackImageBase64().isBlank()
          || response.contentType() == null
          || response.contentType().isBlank()) {
        throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
      }
      log.info(
          "characterizer_request_succeeded provider={} pythonElapsedMs={} clientElapsedMs={}",
          response.provider(),
          response.elapsedMs(),
          elapsedMsSince(startedAt));
      return response.toResult();
    } catch (RestClientException exception) {
      log.warn(
          "characterizer_request_failed baseUrl={} elapsedMs={} exception={}",
          baseUrl,
          elapsedMsSince(startedAt),
          exception.getClass().getSimpleName());
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
  }

  private long elapsedMsSince(long startedAtNanos) {
    return (System.nanoTime() - startedAtNanos) / 1_000_000;
  }

  private HttpCharacterizationRequest createRequestBody(
      String sourceImageUrl, String animalName, CardMetadata metadata) {
    return new HttpCharacterizationRequest(
        sourceImageUrl,
        animalName,
        metadata.cardType().name(),
        metadata.cardTypeLabel(),
        metadata.power(),
        metadata.skill1().displayName(),
        metadata.skill1().description(),
        metadata.skill2().displayName(),
        metadata.skill2().description(),
        metadata.cardNo());
  }

  private record HttpCharacterizationRequest(
      @JsonProperty("source_image_url") String sourceImageUrl,
      @JsonProperty("animal_name") String animalName,
      @JsonProperty("card_type") String cardType,
      @JsonProperty("card_type_label") String cardTypeLabel,
      Integer power,
      @JsonProperty("skill_1_name") String skill1Name,
      @JsonProperty("skill_1_description") String skill1Description,
      @JsonProperty("skill_2_name") String skill2Name,
      @JsonProperty("skill_2_description") String skill2Description,
      @JsonProperty("card_no") String cardNo) {}

  private record HttpCharacterizationResponse(
      String status,
      String provider,
      @JsonProperty("content_type") String contentType,
      @JsonProperty("ai_image_base64") String aiImageBase64,
      @JsonProperty("card_image_base64") String cardImageBase64,
      @JsonProperty("card_back_image_base64") String cardBackImageBase64,
      @JsonProperty("elapsed_ms") Integer elapsedMs) {

    private CharacterizerResult toResult() {
      return new CharacterizerResult(
          status,
          provider,
          contentType,
          aiImageBase64,
          cardImageBase64,
          cardBackImageBase64,
          elapsedMs);
    }
  }
}
