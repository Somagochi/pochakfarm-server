package com.somagochi.pochakfarm.characterization.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.somagochi.pochakfarm.characterization.domain.CardMetadata;
import com.somagochi.pochakfarm.characterization.domain.CardSkill;
import com.somagochi.pochakfarm.characterization.domain.CardType;
import com.somagochi.pochakfarm.characterization.domain.CharacterizerResult;
import com.somagochi.pochakfarm.common.exception.BusinessException;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class HttpCharacterizerClientTest {

  private HttpServer server;
  private String requestBody;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void mapsHttpBase64JsonResponse() throws IOException {
    startServer(
        0,
        """
        {
          "status": "success",
          "provider": "codex_exec",
          "content_type": "image/png",
          "ai_image_base64": "YWk=",
          "card_image_base64": "Y2FyZA==",
          "card_back_image_base64": "YmFjaw==",
          "elapsed_ms": 123
        }
        """);
    HttpCharacterizerClient client =
        new HttpCharacterizerClient(baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1));

    CharacterizerResult result =
        client.characterize("https://cdn.test/original.png", "솜구름", metadata());

    assertEquals("success", result.status());
    assertEquals("codex_exec", result.provider());
    assertEquals("image/png", result.contentType());
    assertEquals("YWk=", result.aiImageBase64());
    assertEquals("Y2FyZA==", result.cardImageBase64());
    assertEquals("YmFjaw==", result.cardBackImageBase64());
    assertEquals(123, result.elapsedMs());
    assertTrue(requestBody.contains("\"source_image_url\":\"https://cdn.test/original.png\""));
    assertTrue(requestBody.contains("\"card_type\":\"SKY\""));
    assertTrue(requestBody.contains("SKY"));
    assertTrue(requestBody.contains("\"card_type_label\":\"하늘\""));
    assertTrue(requestBody.contains("하늘"));
    assertTrue(requestBody.contains("\"power\":82"));
    assertTrue(requestBody.contains("82"));
    assertTrue(requestBody.contains("\"skill_1_name\":\"구름 점프\""));
    assertTrue(requestBody.contains("구름 점프"));
    assertTrue(requestBody.contains("\"skill_2_name\":\"바람 돌진\""));
    assertTrue(requestBody.contains("바람 돌진"));
    assertTrue(requestBody.contains("\"card_no\":\"001\""));
  }

  @Test
  void failsWhenHttpReadExceedsConfiguredTimeout() throws IOException {
    startServer(500, "{\"status\":\"success\"}");
    HttpCharacterizerClient client =
        new HttpCharacterizerClient(baseUrl(), Duration.ofMillis(100), Duration.ofMillis(100));

    assertThrows(
        BusinessException.class,
        () -> client.characterize("https://cdn.test/original.png", "솜구름", metadata()));
  }

  private void startServer(long responseDelayMillis, String responseBody) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/internal/characterize",
        exchange -> {
          try {
            if (responseDelayMillis > 0) {
              Thread.sleep(responseDelayMillis);
            }
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            requestBody =
                new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
              outputStream.write(response);
            }
          } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
          } finally {
            exchange.close();
          }
        });
    server.start();
  }

  private String baseUrl() {
    return "http://127.0.0.1:" + server.getAddress().getPort();
  }

  private static CardMetadata metadata() {
    return new CardMetadata(
        CardType.SKY, 82, CardSkill.SKY_CLOUD_JUMP, CardSkill.SKY_WIND_DASH, "001");
  }
}
