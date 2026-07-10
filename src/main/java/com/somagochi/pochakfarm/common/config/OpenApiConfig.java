package com.somagochi.pochakfarm.common.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  private static final String AUTH_TAG = "Auth";

  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI()
        .info(new Info().title("Pochakfarm API").version("v1").description("포착팜 백엔드 API 문서"))
        .components(
            new Components()
                .addSecuritySchemes(
                    AUTH_TAG,
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("액세스 토큰. 값에는 `Bearer` 접두어 없이 토큰만 입력한다.")));
  }

  /**
   * REST API(OAuth2) 소셜 로그인 엔드포인트는 컨트롤러가 아니라 Spring Security 필터가 처리하므로 springdoc이 자동 스캔하지 못한다. 스웨거에
   * 노출되도록 수동으로 문서를 추가한다.
   */
  @Bean
  OpenApiCustomizer oauth2RestApiCustomizer() {
    return openApi ->
        openApi
            .path("/api/auth/oauth2/{provider}", new PathItem().get(loginStartOperation()))
            .path("/api/auth/oauth2/code/{provider}", new PathItem().get(callbackOperation()));
  }

  private Operation loginStartOperation() {
    return new Operation()
        .addTagsItem(AUTH_TAG)
        .summary("소셜 로그인 시작 (REST API / OAuth2)")
        .description(
            "브라우저를 이 URL로 이동시키면 provider 인가 페이지로 302 리다이렉트된다.<br>"
                + "provider 로그인 후 콜백(`/api/auth/oauth2/code/{provider}`)이 호출되고, 서버가 로그인 처리 후 JWT를 반환한다.<br>"
                + "App SDK 방식(`POST /api/auth/login`)과 달리 클라이언트가 provider 토큰을 직접 다루지 않는다.")
        .addParametersItem(providerPathParameter())
        .responses(
            new ApiResponses()
                .addApiResponse("302", new ApiResponse().description("provider 인가 페이지로 리다이렉트")));
  }

  private Operation callbackOperation() {
    return new Operation()
        .addTagsItem(AUTH_TAG)
        .summary("소셜 로그인 콜백 (provider가 호출)")
        .description(
            "provider가 인가코드와 함께 호출하는 콜백으로 클라이언트가 직접 호출하지 않는다.<br>"
                + "서버가 코드→access token 교환 후 내부 User 매핑/가입하고 SocialLoginResponse(JWT)를 반환한다.")
        .addParametersItem(providerPathParameter())
        .addParametersItem(queryParameter("code", "provider 인가코드"))
        .addParametersItem(queryParameter("state", "CSRF 방지용 state 값"))
        .responses(
            new ApiResponses()
                .addApiResponse(
                    "200", new ApiResponse().description("로그인 성공 (SocialLoginResponse)")));
  }

  private Parameter providerPathParameter() {
    return new Parameter()
        .name("provider")
        .in("path")
        .required(true)
        .description("소셜 provider")
        .schema(new StringSchema()._enum(List.of("kakao", "naver", "apple")).example("kakao"));
  }

  private Parameter queryParameter(String name, String description) {
    return new Parameter()
        .name(name)
        .in("query")
        .required(true)
        .description(description)
        .schema(new StringSchema());
  }
}
