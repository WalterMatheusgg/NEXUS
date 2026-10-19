package com.nexuscrm.processos;

import com.nexuscrm.clientes.DomainSupport;
import com.nexuscrm.comercial.HistoryService;
import com.nexuscrm.processos.ProcessRequests.*;
import com.nexuscrm.shared.*;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProcessService {
  private static final Set<String> FINISHED_STEPS = Set.of("APPROVED", "COMPLETED");
  private final CorporateProcessRepository processes;
  private final ProcessStepRepository steps;
  private final DomainSupport support;
  private final HistoryService history;

  public ProcessService(
      CorporateProcessRepository processes,
      ProcessStepRepository steps,
      DomainSupport support,
      HistoryService history) {
    this.processes = processes;
    this.steps = steps;
    this.support = support;
    this.history = history;
  }

  public CorporateProcess require(Long id) {
    return support.get(processes, id, "Processo");
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> list(
      String search,
      String status,
      Long assigneeId,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    support.current.require("PROCESSES_READ");
    return PageResponse.from(
        processes
            .findAll(
                support.filter(
                    search,
                    List.of("title", "description"),
                    status,
                    "status",
                    assigneeId,
                    from,
                    to),
                pageable)
            .map(this::view));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id) {
    support.current.require("PROCESSES_READ");
    var value = view(require(id));
    value.put("steps", steps(id));
    return value;
  }

  public Map<String, Object> create(ProcessInput input) {
    support.current.require("PROCESSES_WRITE");
    if ("COMPLETED".equals(input.status()))
      throw ApiException.badRequest("Um processo deve ser criado antes da conclusão das etapas.");
    var entity = support.initialize(new CorporateProcess());
    apply(entity, input);
    processes.saveAndFlush(entity);
    history.record("PROCESS", entity.getId(), "CREATE", "Processo cadastrado");
    support.notifyAssignment(
        entity.getAssigneeId(), entity.getTitle(), "/processes/" + entity.getId());
    return view(entity);
  }

  public Map<String, Object> update(Long id, ProcessInput input) {
    support.current.require("PROCESSES_WRITE");
    var entity = require(id);
    support.version(entity, input.version());
    if ("COMPLETED".equals(input.status())) {
      var all =
          steps.findByOrganizationIdAndProcessIdOrderByPosition(
              support.current.organizationId(), id);
      if (all.isEmpty()
          || all.stream().anyMatch(step -> !FINISHED_STEPS.contains(step.getStatus())))
        throw ApiException.conflict(
            "Conclua e aprove todas as etapas antes de finalizar o processo.");
    }
    Long old = entity.getAssigneeId();
    String previous = entity.getStatus();
    apply(entity, input);
    processes.saveAndFlush(entity);
    history.record(
        "PROCESS",
        id,
        "UPDATE",
        "Processo atualizado; situação " + previous + " → " + entity.getStatus());
    if (!Objects.equals(old, entity.getAssigneeId()))
      support.notifyAssignment(entity.getAssigneeId(), entity.getTitle(), "/processes/" + id);
    return view(entity);
  }

  private void apply(CorporateProcess entity, ProcessInput input) {
    support.member(input.assigneeId());
    entity.setTitle(input.title().strip());
    entity.setDescription(input.description());
    entity.setStatus(input.status());
    entity.setAssigneeId(input.assigneeId());
  }

  public Map<String, Object> view(CorporateProcess entity) {
    var value = support.view(entity);
    value.put("title", entity.getTitle());
    value.put("description", entity.getDescription());
    value.put("status", entity.getStatus());
    value.put("assigneeId", entity.getAssigneeId());
    value.put("assigneeName", support.memberName(entity.getAssigneeId()));
    return value;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> steps(Long id) {
    support.current.require("PROCESSES_READ");
    require(id);
    return steps
        .findByOrganizationIdAndProcessIdOrderByPosition(support.current.organizationId(), id)
        .stream()
        .map(this::stepView)
        .toList();
  }

  public Map<String, Object> addStep(Long id, StepInput input) {
    support.current.require("PROCESSES_WRITE");
    var process = require(id);
    ensureMutable(process);
    support.member(input.assigneeId());
    if (input.requiresApproval() && input.assigneeId() == null)
      throw ApiException.badRequest("Etapas de aprovação exigem um responsável.");
    var all =
        steps.findByOrganizationIdAndProcessIdOrderByPosition(support.current.organizationId(), id);
    var entity = support.initialize(new ProcessStep());
    entity.setProcessId(id);
    entity.setTitle(input.title().strip());
    entity.setAssigneeId(input.assigneeId());
    entity.setRequiresApproval(input.requiresApproval());
    entity.setPosition(all.size() + 1);
    entity.setStatus("PENDING");
    steps.saveAndFlush(entity);
    history.record("PROCESS", id, "STEP_CREATE", "Etapa adicionada: " + entity.getTitle());
    support.notifyAssignment(entity.getAssigneeId(), entity.getTitle(), "/processes/" + id);
    return stepView(entity);
  }

  public Map<String, Object> stepStatus(Long id, Long stepId, StepStatusInput input) {
    support.current.require("PROCESSES_WRITE");
    var process = require(id);
    ensureMutable(process);
    var entity =
        steps
            .findByIdAndOrganizationIdAndProcessId(stepId, support.current.organizationId(), id)
            .orElseThrow(() -> ApiException.notFound("Etapa não encontrada."));
    support.version(entity, input.version());
    var all =
        steps.findByOrganizationIdAndProcessIdOrderByPosition(support.current.organizationId(), id);
    if (all.stream()
        .anyMatch(
            step ->
                step.getPosition() < entity.getPosition()
                    && !FINISHED_STEPS.contains(step.getStatus())))
      throw ApiException.conflict("Conclua as etapas anteriores primeiro.");
    if (all.stream()
            .anyMatch(
                step ->
                    step.getPosition() > entity.getPosition()
                        && !"PENDING".equals(step.getStatus()))
        && !entity.getStatus().equals(input.status()))
      throw ApiException.conflict("Etapas posteriores já iniciadas impedem reabrir esta etapa.");
    if (Set.of("APPROVED", "REJECTED").contains(input.status())) {
      if (!entity.getRequiresApproval())
        throw ApiException.badRequest("Esta etapa não exige aprovação.");
      if (!Objects.equals(entity.getAssigneeId(), support.current.userId())
          && !support.administrator())
        throw ApiException.forbidden(
            "Somente o responsável ou administrador pode aprovar ou rejeitar esta etapa.");
      entity.setApprovedBy(support.current.userId());
    } else {
      if ("COMPLETED".equals(input.status()) && entity.getRequiresApproval())
        throw ApiException.badRequest("Esta etapa precisa de aprovação pelo responsável.");
      entity.setApprovedBy(null);
    }
    String previous = entity.getStatus();
    entity.setStatus(input.status());
    entity.setCompletedAt(
        FINISHED_STEPS.contains(input.status()) ? OffsetDateTime.now(ZoneOffset.UTC) : null);
    steps.saveAndFlush(entity);
    if ("OPEN".equals(process.getStatus()) && !"PENDING".equals(input.status())) {
      process.setStatus("IN_PROGRESS");
      processes.save(process);
    }
    history.record(
        "PROCESS", id, "STEP_STATUS", entity.getTitle() + ": " + previous + " → " + input.status());
    support.notifyAssignment(
        process.getAssigneeId(), "Etapa atualizada: " + entity.getTitle(), "/processes/" + id);
    return stepView(entity);
  }

  private void ensureMutable(CorporateProcess process) {
    if (Set.of("COMPLETED", "CANCELLED").contains(process.getStatus()))
      throw ApiException.conflict("Reabra o processo antes de alterar suas etapas.");
  }

  private Map<String, Object> stepView(ProcessStep entity) {
    var value = support.view(entity);
    value.put("title", entity.getTitle());
    value.put("assigneeId", entity.getAssigneeId());
    value.put("assigneeName", support.memberName(entity.getAssigneeId()));
    value.put("requiresApproval", entity.getRequiresApproval());
    value.put("position", entity.getPosition());
    value.put("status", entity.getStatus());
    value.put("approvedBy", entity.getApprovedBy());
    value.put("completedAt", entity.getCompletedAt());
    return value;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> history(Long id) {
    support.current.require("PROCESSES_READ");
    require(id);
    return history.list("PROCESS", id);
  }
}
