package com.somagochi.pochakfarm.characterization.infrastructure;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerClient;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import java.io.IOException;
import java.time.Duration;
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
public class FastApiCharacterizerClient implements CharacterizerClient {

  private final RestClient restClient;

  public FastApiCharacterizerClient(
      @Value("${app.characterizer.base-url:http://localhost:8000}") String baseUrl,
      @Value("${app.characterizer.connect-timeout:PT5S}") Duration connectTimeout,
      @Value("${app.characterizer.read-timeout:PT6M}") Duration readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);
    this.restClient = RestClient.builder().baseUrl(baseUrl).requestFactory(requestFactory).build();
  }

  @Override
  public CharacterizerResult characterize(MultipartFile image, String animalName) {
    try {
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
      return response.toResult();
    } catch (IOException | RestClientException exception) {
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
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
