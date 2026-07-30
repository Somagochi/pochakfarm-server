package com.somagochi.pochakfarm.capture.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerRequest;
import com.somagochi.pochakfarm.capture.domain.CaptureCharacterizerResult;
import com.somagochi.pochakfarm.capture.domain.Tier;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.somagochi.pochakfarm.common.exception.ErrorCode;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpCaptureCharacterizerClientTest {

  private HttpServer server;
  private String requestBody;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void postsCaptureCharacterizeRequestAndMapsResponse() throws IOException {
    startServer(
        200,
        """
        {
          "status": "success",
          "provider": "openai",
          "scene_content_type": "image/png",
          "card_content_type": "image/png",
          "elapsed_ms": 18420
        }
        """);
    HttpCaptureCharacterizerClient client =
        new HttpCaptureCharacterizerClient(baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1));

    CaptureCharacterizerResult result = client.characterize(request());

    assertEquals("success", result.status());
    assertEquals("openai", result.provider());
    assertEquals("image/png", result.sceneContentType());
    assertEquals("image/png", result.cardContentType());
    assertEquals(18420, result.elapsedMs());
    assertTrue(requestBody.contains("\"original_image_download_url\":\"https://download.test/o\""));
    assertTrue(requestBody.contains("\"scene_image_upload_url\":\"https://upload.test/scene\""));
    assertTrue(requestBody.contains("\"card_image_upload_url\":\"https://upload.test/card\""));
    assertTrue(requestBody.contains("\"animal_name\":\"두부\""));
    assertTrue(requestBody.contains("\"card_type\":\"GROUND\""));
    assertTrue(requestBody.contains("\"card_type_label\":\"땅\""));
    assertTrue(requestBody.contains("\"tier\":\"S\""));
    assertTrue(requestBody.contains("\"skill_1_name\":\"냥냥 펀치\""));
    assertTrue(requestBody.contains("\"skill_2_name\":\"나뭇잎 방어\""));
    assertTrue(requestBody.contains("\"card_no\":\"123\""));
  }

  @Test
  void mapsUnsupportedImageError() throws IOException {
    startServer(
        422,
        """
        {
          "status": "failed",
          "error_code": "UNSUPPORTED_CHARACTERIZATION_IMAGE",
          "message": "Unsupported characterization image"
        }
        """);
    HttpCaptureCharacterizerClient client =
        new HttpCaptureCharacterizerClient(baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1));

    BusinessException exception =
        assertThrows(BusinessException.class, () -> client.characterize(request()));

    assertEquals(ErrorCode.UNSUPPORTED_CHARACTERIZATION_IMAGE.getCode(), exception.getCode());
  }

  private void startServer(int statusCode, String responseBody) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/captures/characterize",
        exchange -> {
          byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
          requestBody =
              new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(statusCode, response.length);
          try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(response);
          } finally {
            exchange.close();
          }
        });
    server.start();
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private CaptureCharacterizerRequest request() {
    return new CaptureCharacterizerRequest(
        "https://download.test/o",
        "https://upload.test/scene",
        "https://upload.test/card",
        "두부",
        CardType.GROUND,
        Tier.S,
        CardSkill.GROUND_PAW_STRIKE,
        CardSkill.GROUND_LEAF_GUARD,
        "123");
  }
}
