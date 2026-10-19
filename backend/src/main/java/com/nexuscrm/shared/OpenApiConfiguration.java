package com.nexuscrm.shared;

import io.swagger.v3.oas.models.*;
import io.swagger.v3.oas.models.info.*;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.*;

@Configuration
public class OpenApiConfiguration {
  @Bean
  OpenAPI nexusApi() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Nexus CRM API")
                .version("1.0.0")
                .description(
                    "CRM multiempresa. Autentique em POST /auth/login após GET /auth/csrf. A sessão"
                        + " usa cookie HttpOnly NEXUSSESSION; mutações exigem X-XSRF-TOKEN"
                        + " correspondente ao cookie XSRF-TOKEN. A organização é obtida da sessão,"
                        + " nunca de parâmetros do cliente. Consulte /auth/me para as permissões"
                        + " atuais. Listas usam paginação baseada em zero e retornam PageResponse."
                        + " Erros seguem application/problem+json."))
        .components(
            new Components()
                .addSecuritySchemes(
                    "session",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("NEXUSSESSION"))
                .addSecuritySchemes(
                    "csrf",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.HEADER)
                        .name("X-XSRF-TOKEN")));
  }
}
