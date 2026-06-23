package com.somagochi.pochakfarm.common.logging;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.Setter;

public class DiscordLogbackAppender extends AppenderBase<ILoggingEvent> {

  private static final int DISCORD_CONTENT_LIMIT = 2000;

  @Setter private String webhookUrl;
  private HttpClient httpClient;

  @Override
  public void start() {
    if (webhookUrl == null || webhookUrl.isBlank()) {
      addWarn("Discord webhookUrl is not configured; appender disabled.");
      return;
    }
    this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    super.start();
  }

  @Override
  protected void append(ILoggingEvent event) {
    try {
      String payload =
          buildPayload(event.getLoggerName(), event.getLevel().toString(), buildMessage(event));
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(webhookUrl))
              .timeout(Duration.ofSeconds(5))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
              .build();
      HttpResponse<Void> response =
          httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      if (response.statusCode() >= 300) {
        addWarn("Discord webhook returned status " + response.statusCode());
      }
    } catch (InterruptedException exception) {
      Thread.currentThread().interrupt();
      addError("Interrupted while sending log to Discord webhook", exception);
    } catch (Exception exception) {
      addError("Failed to send log to Discord webhook", exception);
    }
  }

  private static String buildMessage(ILoggingEvent event) {
    String message = event.getFormattedMessage();
    if (event.getThrowableProxy() != null) {
      message = message + "\n" + event.getThrowableProxy().getClassName();
    }
    return message;
  }

  static String buildPayload(String logger, String level, String message) {
    String content = "[" + level + "] " + logger + "\n" + message;
    if (content.length() > DISCORD_CONTENT_LIMIT) {
      content = content.substring(0, DISCORD_CONTENT_LIMIT - 3) + "...";
    }
    return "{\"content\":\"" + escapeJson(content) + "\"}";
  }

  private static String escapeJson(String value) {
    StringBuilder builder = new StringBuilder(value.length() + 16);
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '"' -> builder.append("\\\"");
        case '\\' -> builder.append("\\\\");
        case '\n' -> builder.append("\\n");
        case '\r' -> builder.append("\\r");
        case '\t' -> builder.append("\\t");
        default -> {
          if (c < 0x20) {
            builder.append(String.format("\\u%04x", (int) c));
          } else {
            builder.append(c);
          }
        }
      }
    }
    return builder.toString();
  }
}
