package com.nexuscrm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "server.address=127.0.0.1")
class NexusCrmApplicationTests {

  @Autowired private DataSource dataSource;

  @Autowired private JdbcTemplate jdbc;

  @Autowired private Flyway flyway;

  @LocalServerPort private int port;

  private final HttpClient http =
      HttpClient.newBuilder()
          .connectTimeout(Duration.ofSeconds(5))
          .followRedirects(HttpClient.Redirect.NEVER)
          .build();

  @Test
  void runsAgainstOracleWithTheApplicationAccountAndValidatedMigrations() throws Exception {
    try (Connection connection = dataSource.getConnection()) {
      assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("Oracle");
      assertThat(connection.getMetaData().getUserName()).isNotIn("SYS", "SYSTEM");
    }
    assertThat(flyway.validateWithResult().validationSuccessful).isTrue();
    assertThat(flyway.info().pending()).isEmpty();
    assertThat(flyway.info().applied())
        .extracting(MigrationInfo::getScript)
        .contains("V1__create_organizations.sql");
    assertThat(flyway.info().applied())
        .allSatisfy(migration -> assertThat(migration.getState().isFailed()).isFalse());
  }

  @Test
  void organizationsTableAndUpdateTriggerAreValidAndEnabled() {
    assertThat(
            jdbc.queryForObject(
                """
                SELECT STATUS FROM USER_OBJECTS
                WHERE OBJECT_NAME = 'ORGANIZATIONS' AND OBJECT_TYPE = 'TABLE'
                """,
                String.class))
        .isEqualTo("VALID");
    assertThat(
            jdbc.queryForObject(
                """
                SELECT STATUS FROM USER_OBJECTS
                WHERE OBJECT_NAME = 'TRG_ORGANIZATIONS_UPDATED_AT' AND OBJECT_TYPE = 'TRIGGER'
                """,
                String.class))
        .isEqualTo("VALID");
    assertThat(
            jdbc.queryForObject(
                """
                SELECT STATUS FROM USER_TRIGGERS
                WHERE TRIGGER_NAME = 'TRG_ORGANIZATIONS_UPDATED_AT'
                """,
                String.class))
        .isEqualTo("ENABLED");
  }

  @Test
  @Transactional
  void oracleGeneratesIdentityAndDefaultsAndMaintainsUpdateTimestamp() {
    String code = uniqueCode();
    jdbc.update("INSERT INTO ORGANIZATIONS (CODE, NAME) VALUES (?, ?)", code, "Integration test");
    OrganizationRow created = organization(code);
    assertThat(created.id()).isPositive();
    assertThat(created.status()).isEqualTo("ACTIVE");
    assertThat(created.createdAt()).isNotNull();
    assertThat(created.updatedAt()).isNotNull();

    // A deliberately old value proves the database trigger overrides client timestamps.
    jdbc.update(
        """
        UPDATE ORGANIZATIONS SET NAME = ?, UPDATED_AT =
        TO_TIMESTAMP_TZ('2000-01-01 00:00:00 +00:00', 'YYYY-MM-DD HH24:MI:SS TZH:TZM')
        WHERE CODE = ?
        """,
        "Updated integration test",
        code);
    OrganizationRow updated = organization(code);
    assertThat(updated.id()).isEqualTo(created.id());
    assertThat(updated.createdAt().toInstant()).isEqualTo(created.createdAt().toInstant());
    assertThat(updated.updatedAt().toInstant()).isAfterOrEqualTo(created.updatedAt().toInstant());
    assertThat(updated.updatedAt().toInstant()).isAfter(Instant.parse("2000-01-01T00:00:00Z"));
  }

  @Test
  @Transactional
  void rejectsDuplicateOrganizationCodes() {
    String code = uniqueCode();
    jdbc.update("INSERT INTO ORGANIZATIONS (CODE, NAME) VALUES (?, ?)", code, "First organization");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ORGANIZATIONS (CODE, NAME) VALUES (?, ?)",
                    code,
                    "Duplicate organization"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  @Transactional
  void rejectsInvalidOrganizationStatus() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ORGANIZATIONS (CODE, NAME, STATUS) VALUES (?, ?, ?)",
                    uniqueCode(),
                    "Invalid status",
                    "INVALID"))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @ParameterizedTest
  @ValueSource(strings = {"CODE", "NAME", "STATUS"})
  @Transactional
  void rejectsMissingRequiredOrganizationFields(String missingField) {
    String code = "CODE".equals(missingField) ? null : uniqueCode();
    String name = "NAME".equals(missingField) ? null : "Missing field";
    String status = "STATUS".equals(missingField) ? null : "ACTIVE";
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO ORGANIZATIONS (CODE, NAME, STATUS) VALUES (?, ?, ?)",
                    code,
                    name,
                    status))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void healthIsPublicAndContainsOnlyTheAggregateStatus() throws Exception {
    HttpResponse<String> response = request("GET", "/actuator/health");
    assertThat(response.statusCode()).isEqualTo(200);
    assertThat(response.headers().firstValue("content-type"))
        .hasValueSatisfying(contentType -> assertThat(contentType).startsWith("application/json"));
    assertThat(response.body())
        .contains("\"status\":\"UP\"")
        .doesNotContain("components", "jdbc:", "password");
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/",
        "/organizations",
        "/actuator",
        "/actuator/health/db",
        "/actuator/health/not-a-probe",
        "/actuator/env",
        "/v3/api-docs",
        "/swagger-ui/index.html"
      })
  void deniesOtherAnonymousRoutes(String path) throws Exception {
    assertThat(request("GET", path).statusCode()).isEqualTo(401);
  }

  @ParameterizedTest
  @ValueSource(strings = {"HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"})
  void deniesNonGetRequestsToHealth(String method) throws Exception {
    assertThat(request(method, "/actuator/health").statusCode())
        .isEqualTo(method.equals("HEAD") || method.equals("OPTIONS") ? 401 : 403);
  }

  private HttpResponse<String> request(String method, String path) throws Exception {
    HttpRequest request =
        HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
            .timeout(Duration.ofSeconds(10))
            .header("Accept", "application/json")
            .method(method, HttpRequest.BodyPublishers.noBody())
            .build();
    return http.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private OrganizationRow organization(String code) {
    return jdbc.queryForObject(
        """
        SELECT ID, STATUS, CREATED_AT, UPDATED_AT FROM ORGANIZATIONS WHERE CODE = ?
        """,
        (row, index) ->
            new OrganizationRow(
                row.getLong("ID"),
                row.getString("STATUS"),
                row.getObject("CREATED_AT", OffsetDateTime.class),
                row.getObject("UPDATED_AT", OffsetDateTime.class)),
        code);
  }

  private String uniqueCode() {
    return "TEST_" + UUID.randomUUID();
  }

  private record OrganizationRow(
      long id, String status, OffsetDateTime createdAt, OffsetDateTime updatedAt) {}
}
