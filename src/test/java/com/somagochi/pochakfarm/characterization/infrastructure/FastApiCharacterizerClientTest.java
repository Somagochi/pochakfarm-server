package com.somagochi.pochakfarm.characterization.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import org.springframework.mock.web.MockMultipartFile;

class FastApiCharacterizerClientTest {

  private HttpServer server;

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void mapsFastApiBase64JsonResponse() throws IOException {
    startServer(
        0,
        """
        {
          "status": "success",
          "provider": "codex_exec",
          "fallback_from": null,
          "animal_name": "솜구름",
          "content_type": "image/png",
          "image_base64": "cmVzdWx0",
          "elapsed_ms": 123
        }
        """);
    FastApiCharacterizerClient client =
        new FastApiCharacterizerClient(baseUrl(), Duration.ofSeconds(1), Duration.ofSeconds(1));

    CharacterizerResult result = client.characterize(image(), "솜구름");

    assertEquals("success", result.status());
    assertEquals("codex_exec", result.provider());
    assertEquals("image/png", result.contentType());
    assertEquals("cmVzdWx0", result.imageBase64());
    assertEquals(123, result.elapsedMs());
  }

  @Test
  void failsWhenFastApiReadExceedsConfiguredTimeout() throws IOException {
    startServer(500, "{\"status\":\"success\"}");
    FastApiCharacterizerClient client =
        new FastApiCharacterizerClient(baseUrl(), Duration.ofMillis(100), Duration.ofMillis(100));

    assertThrows(BusinessException.class, () -> client.characterize(image(), "솜구름"));
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

  private static MockMultipartFile image() {
    return new MockMultipartFile("image", "animal.png", "image/png", "fake-image".getBytes());
  }
}
