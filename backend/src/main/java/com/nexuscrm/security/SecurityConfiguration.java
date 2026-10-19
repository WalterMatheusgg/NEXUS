package com.nexuscrm.security;

import com.nexuscrm.administracao.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.*;
import org.springframework.security.web.csrf.*;

@Configuration
public class SecurityConfiguration {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http,
      CsrfTokenRepository csrf,
      SecurityContextRepository contexts,
      UserAccountRepository users,
      OrganizationRepository organizations)
      throws Exception {
    http.authorizeHttpRequests(
            requests ->
                requests
                    .requestMatchers(
                        HttpMethod.GET,
                        "/actuator/health",
                        "/actuator/health/liveness",
                        "/actuator/health/readiness",
                        "/auth/csrf",
                        "/auth/status")
                    .permitAll()
                    .requestMatchers(
                        HttpMethod.POST,
                        "/auth/setup",
                        "/auth/login",
                        "/auth/forgot-password",
                        "/auth/reset-password")
                    .permitAll()
                    .requestMatchers("/actuator/**")
                    .denyAll()
                    .anyRequest()
                    .authenticated())
        .csrf(
            config ->
                config
                    .csrfTokenRepository(csrf)
                    .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .securityContext(config -> config.securityContextRepository(contexts))
        .sessionManagement(
            config -> config.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .requestCache(config -> config.disable())
        .exceptionHandling(
            config ->
                config
                    .authenticationEntryPoint(
                        (request, response, error) ->
                            SecurityErrors.write(
                                response, 401, "Entre na sua conta para continuar."))
                    .accessDeniedHandler(
                        (request, response, error) ->
                            SecurityErrors.write(
                                response,
                                403,
                                "Acesso negado ou token CSRF inválido. Atualize a página.")))
        .logout(
            config ->
                config
                    .logoutUrl("/auth/logout")
                    .logoutSuccessHandler((request, response, auth) -> response.setStatus(204))
                    .invalidateHttpSession(true)
                    .clearAuthentication(true)
                    .deleteCookies("NEXUSSESSION", "XSRF-TOKEN"))
        .addFilterBefore(new AuthRateLimitFilter(), CsrfFilter.class)
        .addFilterAfter(
            new AccountValidityFilter(users, organizations), SecurityContextHolderFilter.class);
    return http.build();
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  CsrfTokenRepository csrfTokenRepository(
      @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
    var repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
    repository.setCookiePath("/");
    repository.setCookieCustomizer(cookie -> cookie.sameSite("Lax").secure(secure));
    return repository;
  }

  @Bean
  UserDetailsService userDetailsService() {
    return username -> {
      throw new UsernameNotFoundException("Authentication is not configured");
    };
  }
}
