package com.somagochi.pochakfarm.characterization.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.io.IOException;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.multipart.MultipartFile;

@Component
@Slf4j
public class FastApiCharacterizerClient implements CharacterizerClient {

  private final RestClient restClient;
  private final String baseUrl;

  public FastApiCharacterizerClient(
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
  public CharacterizerResult characterize(MultipartFile image, String animalName) {
    long startedAt = System.nanoTime();
    try {
      log.info(
          "characterizer_request_started baseUrl={} contentType={} bytes={}",
          baseUrl,
          image.getContentType(),
          image.getSize());
      FastApiCharacterizationResponse response =
          restClient
              .post()
              .uri("/internal/characterize")
              .contentType(MediaType.MULTIPART_FORM_DATA)
              .body(createRequestBody(image, animalName))
              .retrieve()
              .body(FastApiCharacterizationResponse.class);
      if (response == null
          || !"success".equals(response.status())
          || response.imageBase64() == null
          || response.imageBase64().isBlank()
          || response.contentType() == null
          || response.contentType().isBlank()) {
        throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
      }
      log.info(
          "characterizer_request_succeeded provider={} fallbackFrom={} pythonElapsedMs={} clientElapsedMs={}",
          response.provider(),
          response.fallbackFrom(),
          response.elapsedMs(),
          elapsedMsSince(startedAt));
      return response.toResult();
    } catch (IOException | RestClientException exception) {
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

  private MultiValueMap<String, Object> createRequestBody(MultipartFile image, String animalName)
      throws IOException {
    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add("animal_name", animalName);
    body.add(
        "image",
        new ByteArrayResource(image.getBytes()) {
          @Override
          public String getFilename() {
            return image.getOriginalFilename();
          }
        });
    return body;
  }

  private record FastApiCharacterizationResponse(
      String status,
      String provider,
      @JsonProperty("fallback_from") String fallbackFrom,
      @JsonProperty("animal_name") String animalName,
      @JsonProperty("content_type") String contentType,
      @JsonProperty("image_base64") String imageBase64,
      @JsonProperty("elapsed_ms") Integer elapsedMs) {

    private CharacterizerResult toResult() {
      return new CharacterizerResult(
          status, provider, fallbackFrom, animalName, contentType, imageBase64, elapsedMs);
    }
  }
}
