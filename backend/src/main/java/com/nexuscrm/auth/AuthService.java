package com.nexuscrm.auth;

import com.nexuscrm.administracao.*;
import com.nexuscrm.shared.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserAccountRepository users;
  private final OrganizationRepository organizations;
  private final RoleRepository roles;
  private final ProvisioningService provisioning;
  private final InstallationRepository installation;
  private final PasswordEncoder encoder;
  private final PasswordResetRepository resets;
  private final MailOutboxService mail;
  private final AuditService audit;
  private final SessionTracker sessions;
  private final String setupToken, frontendUrl, dummyHash;
  private final int resetMinutes;

  public AuthService(
      UserAccountRepository users,
      OrganizationRepository organizations,
      RoleRepository roles,
      ProvisioningService provisioning,
      InstallationRepository installation,
      PasswordEncoder encoder,
      PasswordResetRepository resets,
      MailOutboxService mail,
      AuditService audit,
      SessionTracker sessions,
      @Value("${nexus.setup-token}") String setupToken,
      @Value("${nexus.frontend-url}") String frontendUrl,
      @Value("${nexus.reset-minutes:30}") int resetMinutes) {
    this.users = users;
    this.organizations = organizations;
    this.roles = roles;
    this.provisioning = provisioning;
    this.installation = installation;
    this.encoder = encoder;
    this.resets = resets;
    this.mail = mail;
    this.audit = audit;
    this.sessions = sessions;
    this.setupToken = setupToken;
    this.frontendUrl = frontendUrl.replaceAll("/$", "");
    this.resetMinutes = Math.max(5, Math.min(resetMinutes, 60));
    dummyHash = encoder.encode(UUID.randomUUID().toString());
  }

  public SetupStatus status() {
    return new SetupStatus(users.count() == 0, setupToken.length() >= 32);
  }

  @Transactional
  public UserAccount setup(AuthRequests.Setup input) {
    if (setupToken.length() < 32)
      throw new ApiException(
          HttpStatus.SERVICE_UNAVAILABLE,
          "Configure NEXUS_SETUP_TOKEN no ambiente antes da instalação inicial.");
    if (!MessageDigest.isEqual(
        setupToken.getBytes(StandardCharsets.UTF_8),
        input.setupToken().getBytes(StandardCharsets.UTF_8)))
      throw ApiException.forbidden("Token de instalação inválido.");
    InstallationState state =
        installation
            .lock()
            .orElseThrow(() -> ApiException.conflict("Estado de instalação indisponível."));
    if (state.initialized || users.count() != 0)
      throw ApiException.conflict("A instalação inicial já foi concluída.");
    UserAccount user =
        provisioning.provision(
            input.organizationCode(),
            input.organizationName(),
            input.name(),
            input.email(),
            input.password());
    state.initialized = true;
    audit.recordFor(
        user,
        "SETUP",
        "ORGANIZATION",
        user.getOrganizationId(),
        "Organização e administrador inicial cadastrados.");
    return user;
  }

  @Transactional
  public Optional<UserAccount> authenticate(AuthRequests.Login input) {
    Optional<Organization> org =
        organizations
            .findByCodeIgnoreCase(input.organizationCode().trim())
            .filter(o -> "ACTIVE".equals(o.getStatus()));
    Optional<UserAccount> found =
        org.flatMap(
                o -> users.findByOrganizationIdAndEmailIgnoreCase(o.getId(), input.email().trim()))
            .flatMap(u -> users.lock(u.getId(), u.getOrganizationId()));
    UserAccount user = found.orElse(null);
    boolean locked =
        user != null
            && user.getLockedUntil() != null
            && user.getLockedUntil().isAfter(OffsetDateTime.now());
    boolean valid =
        encoder.matches(input.password(), user == null ? dummyHash : user.getPasswordHash());
    if (user == null || !user.isActive() || locked || !valid) {
      if (user != null && user.isActive() && !locked) user.loginFailed();
      return Optional.empty();
    }
    user.loginSucceeded();
    audit.recordFor(user, "LOGIN", "USER", user.getId(), "Autenticação concluída.");
    return Optional.of(user);
  }

  public UserView view(UserAccount user) {
    Organization org =
        organizations
            .findById(user.getOrganizationId())
            .orElseThrow(() -> ApiException.notFound("Organização não encontrada."));
    AppRole role =
        roles
            .findByIdAndOrganizationId(user.getRoleId(), user.getOrganizationId())
            .orElseThrow(() -> ApiException.forbidden("Perfil inválido."));
    return new UserView(
        user.getId(),
        user.getName(),
        user.getEmail(),
        org.getId(),
        org.getCode(),
        org.getName(),
        role.getId(),
        role.getName(),
        new TreeSet<>(role.permissions()));
  }

  @Transactional
  public void forgot(AuthRequests.Forgot input) {
    organizations
        .findByCodeIgnoreCase(input.organizationCode().trim())
        .filter(o -> "ACTIVE".equals(o.getStatus()))
        .flatMap(o -> users.findByOrganizationIdAndEmailIgnoreCase(o.getId(), input.email().trim()))
        .filter(UserAccount::isActive)
        .ifPresent(
            user -> {
              resets.invalidate(user.getOrganizationId(), user.getId(), OffsetDateTime.now());
              byte[] random = new byte[32];
              new SecureRandom().nextBytes(random);
              String token = HexFormat.of().formatHex(random);
              PasswordResetToken reset = new PasswordResetToken();
              reset.setOrganizationId(user.getOrganizationId());
              reset.userId = user.getId();
              reset.tokenHash = digest(token);
              reset.expiresAt = OffsetDateTime.now().plusMinutes(resetMinutes);
              resets.save(reset);
              mail.queue(
                  user.getOrganizationId(),
                  user.getEmail(),
                  "Nexus CRM — recuperação de senha",
                  "Uma recuperação de senha foi solicitada para sua conta Nexus CRM.\n\nAbra: "
                      + frontendUrl
                      + "/reset-password?token="
                      + token
                      + "\n\nO link expira em "
                      + resetMinutes
                      + " minutos e pode ser usado uma única vez. Se você não solicitou, ignore"
                      + " esta mensagem.");
            });
  }

  @Transactional
  public void reset(AuthRequests.Reset input) {
    Passwords.validate(input.password());
    PasswordResetToken token =
        resets
            .findByTokenHash(digest(input.token()))
            .orElseThrow(() -> ApiException.badRequest("Link inválido ou expirado."));
    if (token.usedAt != null || token.expiresAt.isBefore(OffsetDateTime.now()))
      throw ApiException.badRequest("Link inválido ou expirado.");
    UserAccount user =
        users
            .lock(token.userId, token.getOrganizationId())
            .filter(UserAccount::isActive)
            .orElseThrow(() -> ApiException.badRequest("Link inválido ou expirado."));
    user.setPasswordHash(encoder.encode(input.password()));
    user.clearLoginFailures();
    token.usedAt = OffsetDateTime.now();
    resets.invalidate(user.getOrganizationId(), user.getId(), token.usedAt);
    sessions.invalidateAll(user.getId());
    audit.recordFor(
        user, "PASSWORD_RESET", "USER", user.getId(), "Senha recuperada e sessões revogadas.");
  }

  @Transactional
  public void change(UserAccount actor, AuthRequests.Change input) {
    Passwords.validate(input.password());
    UserAccount user =
        users
            .lock(actor.getId(), actor.getOrganizationId())
            .orElseThrow(() -> ApiException.notFound("Usuário não encontrado."));
    if (!encoder.matches(input.currentPassword(), user.getPasswordHash()))
      throw ApiException.badRequest("Senha atual incorreta.");
    user.setPasswordHash(encoder.encode(input.password()));
    resets.invalidate(user.getOrganizationId(), user.getId(), OffsetDateTime.now());
    sessions.invalidateAll(user.getId());
    audit.recordFor(
        user, "PASSWORD_CHANGED", "USER", user.getId(), "Senha alterada e sessões revogadas.");
  }

  public static String digest(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public record SetupStatus(boolean setupRequired, boolean setupEnabled) {}

  public record UserView(
      Long id,
      String name,
      String email,
      Long organizationId,
      String organizationCode,
      String organizationName,
      Long roleId,
      String roleName,
      Set<String> permissions) {}
}
