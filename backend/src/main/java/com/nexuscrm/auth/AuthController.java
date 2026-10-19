package com.nexuscrm.auth;

import com.nexuscrm.administracao.UserAccount;
import com.nexuscrm.shared.*;
import jakarta.servlet.http.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.*;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {
  private final AuthService service;
  private final CurrentUser current;
  private final SecurityContextRepository contexts;
  private final CsrfTokenRepository csrf;
  private final SessionTracker sessions;

  public AuthController(
      AuthService service,
      CurrentUser current,
      SecurityContextRepository contexts,
      CsrfTokenRepository csrf,
      SessionTracker sessions) {
    this.service = service;
    this.current = current;
    this.contexts = contexts;
    this.csrf = csrf;
    this.sessions = sessions;
  }

  @GetMapping("/csrf")
  public Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken());
  }

  @GetMapping("/status")
  public AuthService.SetupStatus status() {
    return service.status();
  }

  @PostMapping("/setup")
  @ResponseStatus(HttpStatus.CREATED)
  public AuthService.UserView setup(
      @Valid @RequestBody AuthRequests.Setup input,
      HttpServletRequest request,
      HttpServletResponse response) {
    return establish(service.setup(input), request, response);
  }

  @PostMapping("/login")
  public AuthService.UserView login(
      @Valid @RequestBody AuthRequests.Login input,
      HttpServletRequest request,
      HttpServletResponse response) {
    return establish(
        service
            .authenticate(input)
            .orElseThrow(
                () ->
                    new ApiException(
                        HttpStatus.UNAUTHORIZED,
                        "Empresa, e-mail ou senha inválidos; tente novamente mais tarde se o acesso"
                            + " estiver bloqueado.")),
        request,
        response);
  }

  @GetMapping("/me")
  public AuthService.UserView me() {
    return service.view(current.account());
  }

  @PostMapping("/forgot-password")
  public Map<String, String> forgot(@Valid @RequestBody AuthRequests.Forgot input) {
    service.forgot(input);
    return Map.of(
        "message",
        "Se os dados corresponderem a uma conta ativa, enviaremos as instruções de recuperação.");
  }

  @PostMapping("/reset-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void reset(@Valid @RequestBody AuthRequests.Reset input) {
    service.reset(input);
  }

  @PostMapping("/change-password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void change(@Valid @RequestBody AuthRequests.Change input, HttpServletRequest request) {
    service.change(current.account(), input);
    SecurityContextHolder.clearContext();
  }

  private AuthService.UserView establish(
      UserAccount user, HttpServletRequest request, HttpServletResponse response) {
    if (request.getSession(false) != null) request.changeSessionId();
    else request.getSession(true);
    var context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(
        UsernamePasswordAuthenticationToken.authenticated(
            new NexusPrincipal(user.getId(), user.getOrganizationId(), user.getAuthVersion()),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    SecurityContextHolder.setContext(context);
    contexts.saveContext(context, request, response);
    sessions.register(user.getId(), request.getSession());
    csrf.saveToken(null, request, response);
    return service.view(user);
  }
}
