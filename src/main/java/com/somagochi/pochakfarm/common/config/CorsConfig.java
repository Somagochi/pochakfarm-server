package com.somagochi.pochakfarm.common.config;

import com.somagochi.pochakfarm.common.properties.OAuth2LoginProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

@Configuration
public class CorsConfig {

  private static final String LOCAL_ORIGIN_PATTERN = "http://localhost:*";
  private static final String APPLE_ORIGIN = "https://appleid.apple.com";
  private static final String ALL_PATHS_PATTERN = "/**";

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      OAuth2LoginProperties oAuth2LoginProperties) {
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration(
        SecurityConfig.OAUTH2_REDIRECTION_BASE_URI, redirectionEndpointConfiguration());
    source.registerCorsConfiguration(
        ALL_PATHS_PATTERN, defaultConfiguration(oAuth2LoginProperties));
    return source;
  }

  private CorsConfiguration redirectionEndpointConfiguration() {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedOrigins(List.of(APPLE_ORIGIN));
    corsConfiguration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
    corsConfiguration.addAllowedHeader("*");
    return corsConfiguration;
  }

  private CorsConfiguration defaultConfiguration(OAuth2LoginProperties oAuth2LoginProperties) {
    CorsConfiguration corsConfiguration = new CorsConfiguration();
    corsConfiguration.setAllowedOriginPatterns(allowedOriginPatterns(oAuth2LoginProperties));
    corsConfiguration.setAllowedMethods(
        List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    corsConfiguration.addAllowedHeader("*");
    corsConfiguration.setAllowCredentials(true);
    return corsConfiguration;
  }

  private List<String> allowedOriginPatterns(OAuth2LoginProperties oAuth2LoginProperties) {
    List<String> patterns = new ArrayList<>();
    patterns.add(LOCAL_ORIGIN_PATTERN);
    originOf(oAuth2LoginProperties.successRedirectUri())
        .filter(origin -> !patterns.contains(origin))
        .ifPresent(patterns::add);
    return List.copyOf(patterns);
  }

  private Optional<String> originOf(String uri) {
    if (uri == null || uri.isBlank()) {
      return Optional.empty();
    }
    UriComponents components = UriComponentsBuilder.fromUriString(uri).build();
    if (components.getScheme() == null || components.getHost() == null) {
      return Optional.empty();
    }
    StringBuilder origin =
        new StringBuilder(components.getScheme()).append("://").append(components.getHost());
    if (components.getPort() != -1) {
      origin.append(":").append(components.getPort());
    }
    return Optional.of(origin.toString());
  }
}
