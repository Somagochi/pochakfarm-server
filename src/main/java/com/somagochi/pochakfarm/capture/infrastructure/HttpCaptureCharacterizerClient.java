package com.somagochi.pochakfarm.capture.infrastructure;

import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerClient;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerRequest;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerResult;
import com.somagochi.pochakfarm.capture.infrastructure.dto.HttpCaptureCharacterizerRequest;
import com.somagochi.pochakfarm.capture.infrastructure.dto.HttpCaptureCharacterizerResponse;
import com.somagochi.pochakfarm.characterization.infrastructure.dto.HttpCharacterizerErrorResponse;
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
import org.springframework.web.client.RestClientResponseException;

@Component
@Slf4j
public class HttpCaptureCharacterizerClient implements CaptureCharacterizerClient {

  private final RestClient restClient;
  private final String baseUrl;

  public HttpCaptureCharacterizerClient(
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
  public CaptureCharacterizerResult characterize(CaptureCharacterizerRequest request) {
    long startedAt = System.nanoTime();
    try {
      HttpCaptureCharacterizerResponse response =
          restClient
              .post()
              .uri("/internal/captures/characterize")
              .contentType(MediaType.APPLICATION_JSON)
              .body(createRequestBody(request))
              .retrieve()
              .body(HttpCaptureCharacterizerResponse.class);
      if (response == null
          || !"success".equals(response.status())
          || response.sceneContentType() == null
          || response.cardContentType() == null) {
        throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
      }
      log.info(
          "capture_characterizer_request_succeeded provider={} elapsedMs={} clientElapsedMs={}",
          response.provider(),
          response.elapsedMs(),
          elapsedMsSince(startedAt));
      return response.toResult();
    } catch (RestClientResponseException exception) {
      ErrorCode errorCode = resolveCharacterizerError(exception);
      log.warn(
          "capture_characterizer_request_failed baseUrl={} elapsedMs={} status={} errorCode={}",
          baseUrl,
          elapsedMsSince(startedAt),
          exception.getStatusCode().value(),
          errorCode.getCode(),
          exception);
      throw new BusinessException(errorCode);
    } catch (RestClientException exception) {
      log.warn(
          "capture_characterizer_request_failed baseUrl={} elapsedMs={} exception={}",
          baseUrl,
          elapsedMsSince(startedAt),
          exception.getClass().getSimpleName(),
          exception);
      throw new BusinessException(ErrorCode.CHARACTERIZATION_FAILED);
    }
  }

  private HttpCaptureCharacterizerRequest createRequestBody(CaptureCharacterizerRequest request) {
    return new HttpCaptureCharacterizerRequest(
        request.originalImageDownloadUrl(),
        request.sceneImageUploadUrl(),
        request.cardImageUploadUrl(),
        request.animalName(),
        request.cardType().name(),
        request.cardType().label(),
        request.tier().name(),
        request.skill1().displayName(),
        request.skill1().description(),
        request.skill2().displayName(),
        request.skill2().description(),
        request.cardNo());
  }

  private ErrorCode resolveCharacterizerError(RestClientResponseException exception) {
    try {
      HttpCharacterizerErrorResponse errorResponse =
          exception.getResponseBodyAs(HttpCharacterizerErrorResponse.class);
      if (errorResponse != null
          && ErrorCode.UNSUPPORTED_CHARACTERIZATION_IMAGE
              .getCode()
              .equals(errorResponse.errorCode())) {
        return ErrorCode.UNSUPPORTED_CHARACTERIZATION_IMAGE;
      }
    } catch (RestClientException parseException) {
      log.debug("capture_characterizer_error_response_parse_failed", parseException);
    }
    return ErrorCode.CHARACTERIZATION_FAILED;
  }

  private long elapsedMsSince(long startedAtNanos) {
    return (System.nanoTime() - startedAtNanos) / 1_000_000;
  }
}
