package com.nexuscrm.auth;

import static org.assertj.core.api.Assertions.*;

import com.nexuscrm.administracao.*;
import java.net.*;
import java.net.http.*;
import java.time.Duration;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.address=127.0.0.1", "nexus.mail.poll-ms=3600000"})
class AuthHttpIntegrationTests {
  @Autowired ProvisioningService provisioning;
  @Autowired JdbcTemplate jdbc;
  @Autowired TransactionTemplate transaction;
  @LocalServerPort int port;
  private final JsonMapper json = JsonMapper.builder().build();
  private final List<Long> organizations = new ArrayList<>();
  private final String password = "Integration-" + UUID.randomUUID();
  private String organizationCode;
  private long adminId;
  private Client admin;

  @BeforeEach
  void createIsolatedFixture() {
    organizationCode = "HTTP_" + UUID.randomUUID().toString().replace("-", "");
    var user =
        provisioning.provision(
            organizationCode,
            "Empresa temporária HTTP",
            "Administrador teste",
            "admin@example.invalid",
            password);
    organizations.add(user.getOrganizationId());
    adminId = user.getId();
    admin = new Client();
  }

  @AfterEach
  void deleteOnlyTestFixtures() {
    transaction.executeWithoutResult(
        status -> {
          for (long organization : organizations) {
            for (String table :
                List.of(
                    "MAIL_OUTBOX",
                    "PASSWORD_RESET_TOKENS",
                    "NOTIFICATIONS",
                    "AUDIT_EVENTS",
                    "PROCESS_STEPS",
                    "CORPORATE_PROCESSES",
                    "WORK_TASKS",
                    "TICKET_ATTACHMENTS",
                    "TICKET_COMMENTS",
                    "SUPPORT_TICKETS",
                    "OPPORTUNITY_INTERACTIONS",
                    "BUSINESS_HISTORY",
                    "OPPORTUNITIES",
                    "SALES_LEADS",
                    "CUSTOMER_INTERACTIONS",
                    "CUSTOMER_CONTACTS",
                    "CUSTOMERS",
                    "APP_USERS",
                    "TEAMS",
                    "DEPARTMENTS",
                    "APP_ROLES"))
              jdbc.update("DELETE FROM " + table + " WHERE ORGANIZATION_ID=?", organization);
            jdbc.update("DELETE FROM ORGANIZATIONS WHERE ID=?", organization);
          }
        });
  }

  @Test
  void sessionsCsrfAndLogoutAreEnforced() throws Exception {
    assertThat(admin.call("GET", "/auth/me", null, false).statusCode()).isEqualTo(401);
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/login",
                    Map.of(
                        "organizationCode",
                        organizationCode,
                        "email",
                        "admin@example.invalid",
                        "password",
                        password),
                    false)
                .statusCode())
        .isEqualTo(403);
    admin.csrf();
    var login = admin.login();
    assertThat(login.statusCode()).isEqualTo(200);
    JsonNode user = json.readTree(login.body());
    assertThat(user.get("permissions").size()).isEqualTo(Permission.all().size());
    assertThat(login.body()).doesNotContain("passwordHash", password);
    assertThat(admin.cookies.getCookieStore().getCookies())
        .anySatisfy(
            cookie -> {
              assertThat(cookie.getName()).isEqualTo("NEXUSSESSION");
              assertThat(cookie.isHttpOnly()).isTrue();
            });
    assertThat(admin.call("GET", "/auth/me", null, false).statusCode()).isEqualTo(200);
    assertThat(admin.call("POST", "/auth/logout", null, true).statusCode()).isEqualTo(204);
    assertThat(admin.call("GET", "/auth/me", null, false).statusCode()).isEqualTo(401);
  }

  @Test
  void tenantIsolationAndRolePermissionsApplyToHttpEndpoints() throws Exception {
    admin.csrf();
    assertThat(admin.login().statusCode()).isEqualTo(200);
    var customer =
        admin.call(
            "POST",
            "/customers",
            Map.of(
                "type",
                "PERSON",
                "name",
                "Cliente HTTP",
                "document",
                "52998224725",
                "active",
                true),
            true);
    assertThat(customer.statusCode()).isEqualTo(201);
    long customerId = json.readTree(customer.body()).get("id").asLong();
    String otherCode = "HTTP_" + UUID.randomUUID().toString().replace("-", "");
    var other =
        provisioning.provision(
            otherCode,
            "Outra organização HTTP",
            "Administrador externo",
            "other@example.invalid",
            password);
    organizations.add(other.getOrganizationId());
    Client otherClient = new Client();
    otherClient.csrf();
    assertThat(
            otherClient
                .call(
                    "POST",
                    "/auth/login",
                    Map.of(
                        "organizationCode",
                        otherCode,
                        "email",
                        "other@example.invalid",
                        "password",
                        password),
                    true)
                .statusCode())
        .isEqualTo(200);
    otherClient.csrf();
    assertThat(otherClient.call("GET", "/customers/" + customerId, null, false).statusCode())
        .isEqualTo(404);
    assertThat(
            json.readTree(otherClient.call("GET", "/customers", null, false).body())
                .get("totalElements")
                .asLong())
        .isZero();
    long employeeRole =
        jdbc.queryForObject(
            "SELECT ID FROM APP_ROLES WHERE ORGANIZATION_ID=? AND CODE='EMPLOYEE'",
            Long.class,
            organizations.getFirst());
    var employee =
        admin.call(
            "POST",
            "/admin/users",
            Map.of(
                "name",
                "Funcionário HTTP",
                "email",
                "employee@example.invalid",
                "password",
                password,
                "roleId",
                employeeRole,
                "active",
                true),
            true);
    assertThat(employee.statusCode()).isEqualTo(201);
    Client limited = new Client();
    limited.csrf();
    assertThat(
            limited
                .call(
                    "POST",
                    "/auth/login",
                    Map.of(
                        "organizationCode",
                        organizationCode,
                        "email",
                        "employee@example.invalid",
                        "password",
                        password),
                    true)
                .statusCode())
        .isEqualTo(200);
    limited.csrf();
    assertThat(limited.call("GET", "/admin/users", null, false).statusCode()).isEqualTo(403);
    assertThat(
            limited
                .call(
                    "POST",
                    "/customers",
                    Map.of(
                        "type",
                        "PERSON",
                        "name",
                        "Não autorizado",
                        "document",
                        "11144477735",
                        "active",
                        true),
                    true)
                .statusCode())
        .isEqualTo(403);
    assertThat(limited.call("GET", "/customers", null, false).statusCode()).isEqualTo(200);
  }

  @Test
  void passwordResetIsGenericSingleUseAndRevokesSessions() throws Exception {
    admin.csrf();
    assertThat(admin.login().statusCode()).isEqualTo(200);
    var known =
        admin.call(
            "POST",
            "/auth/forgot-password",
            Map.of("organizationCode", organizationCode, "email", "admin@example.invalid"),
            true);
    var unknown =
        admin.call(
            "POST",
            "/auth/forgot-password",
            Map.of("organizationCode", organizationCode, "email", "missing@example.invalid"),
            true);
    assertThat(known.statusCode()).isEqualTo(200);
    assertThat(unknown.body()).isEqualTo(known.body());
    String body =
        jdbc.queryForObject(
            "SELECT BODY FROM MAIL_OUTBOX WHERE ORGANIZATION_ID=?",
            String.class,
            organizations.getFirst());
    String token = body.substring(body.indexOf("?token=") + 7).split("\\s", 2)[0];
    assertThat(
            jdbc.queryForObject(
                "SELECT TOKEN_HASH FROM PASSWORD_RESET_TOKENS WHERE ORGANIZATION_ID=?",
                String.class,
                organizations.getFirst()))
        .isEqualTo(AuthService.digest(token));
    String changed = "Changed-" + UUID.randomUUID();
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/reset-password",
                    Map.of("token", token, "password", changed),
                    true)
                .statusCode())
        .isEqualTo(204);
    assertThat(admin.call("GET", "/auth/me", null, false).statusCode()).isEqualTo(401);
    admin.csrf();
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/reset-password",
                    Map.of("token", token, "password", changed),
                    true)
                .statusCode())
        .isEqualTo(400);
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/login",
                    Map.of(
                        "organizationCode",
                        organizationCode,
                        "email",
                        "admin@example.invalid",
                        "password",
                        changed),
                    true)
                .statusCode())
        .isEqualTo(200);
  }

  @Test
  void changingPasswordRequiresCurrentPasswordAndRevokesTheSession() throws Exception {
    admin.csrf();
    assertThat(admin.login().statusCode()).isEqualTo(200);
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/change-password",
                    Map.of("currentPassword", "incorrect", "password", password),
                    true)
                .statusCode())
        .isEqualTo(400);
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/change-password",
                    Map.of("currentPassword", password, "password", "New-" + UUID.randomUUID()),
                    true)
                .statusCode())
        .isEqualTo(204);
    assertThat(admin.call("GET", "/auth/me", null, false).statusCode()).isEqualTo(401);
  }

  @Test
  void delegatedUserAdministrationCannotManageMorePrivilegedAccountsOrRoles() throws Exception {
    admin.csrf();
    assertThat(admin.login().statusCode()).isEqualTo(200);
    var createdRole =
        admin.call(
            "POST",
            "/admin/roles",
            Map.of(
                "code",
                "DELEGATE",
                "name",
                "Delegado",
                "permissions",
                List.of("USERS_MANAGE", "ROLES_MANAGE")),
            true);
    assertThat(createdRole.statusCode()).isEqualTo(201);
    long roleId = json.readTree(createdRole.body()).get("id").asLong();
    var createdUser =
        admin.call(
            "POST",
            "/admin/users",
            Map.of(
                "name",
                "Delegado teste",
                "email",
                "delegate@example.invalid",
                "password",
                password,
                "roleId",
                roleId,
                "active",
                true),
            true);
    assertThat(createdUser.statusCode()).isEqualTo(201);
    Client delegate = new Client();
    delegate.csrf();
    assertThat(
            delegate
                .call(
                    "POST",
                    "/auth/login",
                    Map.of(
                        "organizationCode",
                        organizationCode,
                        "email",
                        "delegate@example.invalid",
                        "password",
                        password),
                    true)
                .statusCode())
        .isEqualTo(200);
    delegate.csrf();
    assertThat(
            delegate
                .call(
                    "PUT",
                    "/admin/users/" + adminId,
                    Map.of(
                        "name",
                        "Admin alterado",
                        "email",
                        "admin@example.invalid",
                        "roleId",
                        roleId,
                        "active",
                        false),
                    true)
                .statusCode())
        .isEqualTo(403);
    assertThat(
            delegate
                .call(
                    "PATCH",
                    "/admin/users/" + adminId + "/password",
                    Map.of("password", password),
                    true)
                .statusCode())
        .isEqualTo(403);
    long adminRole =
        jdbc.queryForObject("SELECT ROLE_ID FROM APP_USERS WHERE ID=?", Long.class, adminId);
    assertThat(
            delegate
                .call(
                    "PUT",
                    "/admin/roles/" + adminRole,
                    Map.of("name", "Administrador", "permissions", List.of("USERS_MANAGE")),
                    true)
                .statusCode())
        .isEqualTo(403);
  }

  @Test
  void administrativePasswordResetInvalidatesPreviouslyIssuedRecoveryLinks() throws Exception {
    admin.csrf();
    assertThat(admin.login().statusCode()).isEqualTo(200);
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/forgot-password",
                    Map.of("organizationCode", organizationCode, "email", "admin@example.invalid"),
                    true)
                .statusCode())
        .isEqualTo(200);
    String body =
        jdbc.queryForObject(
            "SELECT BODY FROM MAIL_OUTBOX WHERE ORGANIZATION_ID=?",
            String.class,
            organizations.getFirst());
    String token = body.substring(body.indexOf("?token=") + 7).split("\\s", 2)[0];
    assertThat(
            admin
                .call(
                    "PATCH",
                    "/admin/users/" + adminId + "/password",
                    Map.of("password", "Admin-" + UUID.randomUUID()),
                    true)
                .statusCode())
        .isEqualTo(204);
    admin.csrf();
    assertThat(
            admin
                .call(
                    "POST",
                    "/auth/reset-password",
                    Map.of("token", token, "password", password),
                    true)
                .statusCode())
        .isEqualTo(400);
  }

  private final class Client {
    final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
    final HttpClient http =
        HttpClient.newBuilder()
            .cookieHandler(cookies)
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    String csrf;

    void csrf() throws Exception {
      var response = call("GET", "/auth/csrf", null, false);
      assertThat(response.statusCode()).isEqualTo(200);
      csrf = json.readTree(response.body()).get("token").asText();
    }

    HttpResponse<String> login() throws Exception {
      var result =
          call(
              "POST",
              "/auth/login",
              Map.of(
                  "organizationCode",
                  organizationCode,
                  "email",
                  "admin@example.invalid",
                  "password",
                  password),
              true);
      if (result.statusCode() == 200) csrf();
      return result;
    }

    HttpResponse<String> call(String method, String path, Object body, boolean protectedRequest)
        throws Exception {
      var builder =
          HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
              .timeout(Duration.ofSeconds(20))
              .header("Accept", "application/json");
      if (protectedRequest && csrf != null) builder.header("X-XSRF-TOKEN", csrf);
      if (body != null) builder.header("Content-Type", "application/json");
      return http.send(
          builder
              .method(
                  method,
                  body == null
                      ? HttpRequest.BodyPublishers.noBody()
                      : HttpRequest.BodyPublishers.ofString(json.writeValueAsString(body)))
              .build(),
          HttpResponse.BodyHandlers.ofString());
    }
  }
}
