package com.nexuscrm.clientes;

import static org.assertj.core.api.Assertions.*;

import com.nexuscrm.administracao.*;
import com.nexuscrm.auth.NexusPrincipal;
import com.nexuscrm.chamados.*;
import com.nexuscrm.chamados.TicketRequests.*;
import com.nexuscrm.clientes.CustomerRequests.*;
import com.nexuscrm.comercial.*;
import com.nexuscrm.comercial.SalesRequests.*;
import com.nexuscrm.dashboard.DashboardService;
import com.nexuscrm.processos.*;
import com.nexuscrm.relatorios.ReportService;
import com.nexuscrm.shared.*;
import com.nexuscrm.tarefas.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class BusinessModulesIntegrationTests {
  @Autowired CustomerService customers;
  @Autowired CustomerRepository customerRepository;
  @Autowired SalesService sales;
  @Autowired TicketService tickets;
  @Autowired TicketRepository ticketRepository;
  @Autowired TaskService tasks;
  @Autowired ProcessService processes;
  @Autowired DashboardService dashboard;
  @Autowired ReportService reports;
  @Autowired OrganizationRepository organizations;
  @Autowired RoleRepository roles;
  @Autowired UserAccountRepository users;
  private UserAccount actor;

  @BeforeEach
  void authenticatedOrganization() {
    actor = createOrganization();
    authenticate(actor);
  }

  @AfterEach
  void clearAuthentication() {
    SecurityContextHolder.clearContext();
  }

  private UserAccount createOrganization() {
    var organization =
        organizations.saveAndFlush(
            new Organization("test-" + UUID.randomUUID(), "Organização de teste de integração"));
    var role = new AppRole();
    role.setOrganizationId(organization.getId());
    role.configure("TEST_ADMIN", "Admin teste", Permission.all());
    roles.saveAndFlush(role);
    var user = new UserAccount();
    user.setOrganizationId(organization.getId());
    user.setName("Usuário de teste");
    user.setEmail("integration@example.invalid");
    user.setPasswordHash("test-only-unused-hash");
    user.setRoleId(role.getId());
    return users.saveAndFlush(user);
  }

  private void authenticate(UserAccount user) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                new NexusPrincipal(user.getId(), user.getOrganizationId(), user.getAuthVersion()),
                null,
                List.of()));
  }

  private Map<String, Object> customer() {
    return customers.create(
        new CustomerInput(
            "PERSON",
            "Cliente de teste",
            "529.982.247-25",
            "client@example.invalid",
            null,
            null,
            true,
            null));
  }

  private long id(Map<String, Object> value) {
    return ((Number) value.get("id")).longValue();
  }

  private TicketInput ticketInput(Long customerId, OffsetDateTime dueAt) {
    return new TicketInput(
        "Chamado integração",
        "Persistência Oracle real",
        customerId,
        "Atendimento",
        "HIGH",
        "OPEN",
        actor.getId(),
        dueAt,
        null);
  }

  @Test
  void documentsUniquenessAndTenantIsolationAreEnforced() {
    var customer = customer();
    long id = id(customer);
    assertThat(customer.get("document")).isEqualTo("52998224725");
    assertThatThrownBy(this::customer)
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("Já existe");
    var second = createOrganization();
    authenticate(second);
    assertThatThrownBy(() -> customers.detail(id))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("não encontrado");
    assertThat(customers.list("", null, null, null, PageRequest.of(0, 20)).content()).isEmpty();
    assertThat(
            customers.create(
                new CustomerInput(
                    "PERSON", "Outra empresa", "52998224725", null, null, null, true, null)))
        .containsEntry("document", "52998224725");
  }

  @Test
  void compositeForeignKeyBlocksCrossOrganizationLinkEvenAtRepositoryLevel() {
    var customer = customer();
    var second = createOrganization();
    var ticket = new Ticket();
    ticket.setOrganizationId(second.getOrganizationId());
    ticket.setCustomerId(id(customer));
    ticket.setNumber(UUID.randomUUID().toString());
    ticket.setTitle("FK teste");
    ticket.setDescription("Teste integridade");
    ticket.setCategory("Teste");
    ticket.setPriority("LOW");
    ticket.setStatus("OPEN");
    assertThatThrownBy(() -> ticketRepository.saveAndFlush(ticket))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void leadConversionCreatesPersistentCustomerAndPreventsDuplicateConversion() {
    var lead =
        sales.createLead(
            new LeadInput(
                "Lead real",
                "lead@example.invalid",
                null,
                "Web",
                "QUALIFIED",
                actor.getId(),
                null,
                null));
    var customer = sales.convert(id(lead), new ConvertInput("52998224725", "PERSON", null));
    assertThat(customers.detail(id(customer))).containsEntry("name", "Lead real");
    assertThat(sales.leadDetail(id(lead)))
        .containsEntry("status", "CONVERTED")
        .containsEntry("customerId", id(customer));
    assertThatThrownBy(
            () -> sales.convert(id(lead), new ConvertInput("11144477735", "PERSON", null)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("já foi convertido");
  }

  @Test
  void opportunitiesHaveRealHistoryAndDashboardAggregates() {
    var opportunity =
        sales.createOpportunity(
            new OpportunityInput(
                "Venda Oracle",
                null,
                null,
                actor.getId(),
                "PROPOSAL",
                new BigDecimal("1250.50"),
                60,
                null,
                null,
                null));
    assertThat((BigDecimal) dashboard.dashboard().get("pipelineValue"))
        .isEqualByComparingTo("1250.50");
    var won =
        sales.stage(
            id(opportunity),
            new StageInput("WON", ((Number) opportunity.get("version")).longValue()));
    assertThat(won).containsEntry("probability", 100);
    assertThat(sales.history(id(opportunity))).hasSize(2);
    assertThat(dashboard.dashboard())
        .containsEntry("openOpportunities", 0L)
        .containsEntry("conversionRate", 100.0);
  }

  @Test
  void ticketsTrackSlaCommentsAndResolutionUsingOracle() {
    var ticket =
        tickets.create(
            ticketInput(id(customer()), OffsetDateTime.now(ZoneOffset.UTC).minusHours(1)));
    assertThat(ticket).containsEntry("overdue", true);
    tickets.addComment(id(ticket), new CommentInput("Primeiro atendimento"));
    assertThat(tickets.comments(id(ticket)))
        .singleElement()
        .satisfies(comment -> assertThat(comment).containsEntry("body", "Primeiro atendimento"));
    var resolved = tickets.status(id(ticket), new StatusInput("RESOLVED", null));
    assertThat(resolved).containsEntry("overdue", false);
    assertThat(resolved.get("resolvedAt")).isNotNull();
    assertThat(tickets.history(id(ticket))).hasSize(3);
    assertThat(dashboard.dashboard())
        .containsEntry("resolvedTickets", 1L)
        .containsEntry("overdueTickets", 0L);
  }

  @Test
  void tasksKeepRealReferencesAndCompletionDates() {
    var customer = customer();
    var ticket = tickets.create(ticketInput(id(customer), null));
    var task =
        tasks.create(
            new TaskRequests.TaskInput(
                "Atender cliente",
                null,
                "HIGH",
                "TODO",
                actor.getId(),
                LocalDate.now(ZoneOffset.UTC).minusDays(1),
                id(customer),
                id(ticket),
                null,
                null));
    assertThat(task).containsEntry("overdue", true);
    var completed = tasks.status(id(task), new TaskRequests.StatusInput("DONE", null));
    assertThat(completed).containsEntry("overdue", false);
    assertThat(completed.get("completedAt")).isNotNull();
    assertThat(dashboard.dashboard()).containsEntry("pendingTasks", 0L);
  }

  @Test
  void processesRequireSequentialCompletionAndResponsibleApproval() {
    var process =
        processes.create(
            new ProcessRequests.ProcessInput(
                "Aprovação comercial", null, "OPEN", actor.getId(), null));
    long processId = id(process);
    var first =
        processes.addStep(
            processId, new ProcessRequests.StepInput("Análise", actor.getId(), false));
    var approval =
        processes.addStep(processId, new ProcessRequests.StepInput("Aprovar", actor.getId(), true));
    assertThatThrownBy(
            () ->
                processes.stepStatus(
                    processId, id(approval), new ProcessRequests.StepStatusInput("APPROVED", null)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("anteriores");
    processes.stepStatus(
        processId, id(first), new ProcessRequests.StepStatusInput("COMPLETED", null));
    assertThatThrownBy(
            () ->
                processes.stepStatus(
                    processId,
                    id(approval),
                    new ProcessRequests.StepStatusInput("COMPLETED", null)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("aprovação");
    processes.stepStatus(
        processId, id(approval), new ProcessRequests.StepStatusInput("APPROVED", null));
    assertThat(
            processes.update(
                processId,
                new ProcessRequests.ProcessInput(
                    "Aprovação comercial", null, "COMPLETED", actor.getId(), null)))
        .containsEntry("status", "COMPLETED");
    assertThat(processes.history(processId)).hasSize(6);
  }

  @Test
  void delegatedUserManagementCannotOverrideAnotherPersonsApproval() {
    var process =
        processes.create(
            new ProcessRequests.ProcessInput(
                "Aprovação restrita", null, "OPEN", actor.getId(), null));
    var step =
        processes.addStep(
            id(process), new ProcessRequests.StepInput("Decisão", actor.getId(), true));
    var role = new AppRole();
    role.setOrganizationId(actor.getOrganizationId());
    role.configure(
        "DELEGATED",
        "Gestão delegada",
        Set.of("PROCESSES_READ", "PROCESSES_WRITE", "USERS_MANAGE"));
    roles.saveAndFlush(role);
    var delegate = new UserAccount();
    delegate.setOrganizationId(actor.getOrganizationId());
    delegate.setName("Delegado");
    delegate.setEmail("delegate@example.invalid");
    delegate.setPasswordHash("test-only-unused-hash");
    delegate.setRoleId(role.getId());
    users.saveAndFlush(delegate);
    authenticate(delegate);
    assertThatThrownBy(
            () ->
                processes.stepStatus(
                    id(process), id(step), new ProcessRequests.StepStatusInput("APPROVED", null)))
        .isInstanceOf(ApiException.class)
        .hasMessageContaining("responsável");
  }

  @Test
  void reportExportsOnlyCurrentOrganizationAndEscapesSpreadsheetFormulas() {
    customers.create(
        new CustomerInput(
            "PERSON",
            "=HYPERLINK(\"https://invalid.example\")",
            "52998224725",
            null,
            null,
            null,
            true,
            null));
    var report = reports.generate("customers", null, null, null, null);
    assertThat(report.total()).isEqualTo(1);
    String csv = new String(reports.csv(report), StandardCharsets.UTF_8);
    assertThat(csv).contains("'=HYPERLINK");
    assertThat(
            reports
                .generate(
                    "customers",
                    LocalDate.now().plusDays(1),
                    LocalDate.now().plusDays(2),
                    null,
                    null)
                .rows())
        .isEmpty();
    authenticate(createOrganization());
    assertThat(reports.generate("customers", null, null, null, null).rows()).isEmpty();
  }
}
