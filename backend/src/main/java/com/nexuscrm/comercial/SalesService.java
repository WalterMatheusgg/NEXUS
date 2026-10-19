package com.nexuscrm.comercial;

import com.nexuscrm.clientes.*;
import com.nexuscrm.clientes.CustomerRequests.*;
import com.nexuscrm.comercial.SalesRequests.*;
import com.nexuscrm.shared.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SalesService {
  private final LeadRepository leads;
  private final OpportunityRepository opportunities;
  private final OpportunityInteractionRepository interactions;
  private final CustomerService customers;
  private final DomainSupport support;
  private final HistoryService history;

  public SalesService(
      LeadRepository leads,
      OpportunityRepository opportunities,
      OpportunityInteractionRepository interactions,
      CustomerService customers,
      DomainSupport support,
      HistoryService history) {
    this.leads = leads;
    this.opportunities = opportunities;
    this.interactions = interactions;
    this.customers = customers;
    this.support = support;
    this.history = history;
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> leads(
      String search,
      String status,
      Long assigneeId,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    support.current.require("SALES_READ");
    return PageResponse.from(
        leads
            .findAll(
                support.filter(
                    search,
                    List.of("name", "email", "source"),
                    status,
                    "status",
                    assigneeId,
                    from,
                    to),
                pageable)
            .map(this::leadView));
  }

  public Lead requireLead(Long id) {
    return support.get(leads, id, "Lead");
  }

  public Opportunity requireOpportunity(Long id) {
    return support.get(opportunities, id, "Oportunidade");
  }

  @Transactional(readOnly = true)
  public Map<String, Object> leadDetail(Long id) {
    support.current.require("SALES_READ");
    return leadView(requireLead(id));
  }

  public Map<String, Object> createLead(LeadInput input) {
    support.current.require("SALES_WRITE");
    if ("CONVERTED".equals(input.status()))
      throw ApiException.badRequest("Use a operação de conversão para converter leads.");
    var entity = support.initialize(new Lead());
    applyLead(entity, input);
    leads.saveAndFlush(entity);
    history.record("LEAD", entity.getId(), "CREATE", "Lead cadastrado");
    support.notifyAssignment(entity.getAssigneeId(), entity.getName(), "/leads");
    return leadView(entity);
  }

  public Map<String, Object> updateLead(Long id, LeadInput input) {
    support.current.require("SALES_WRITE");
    var entity = requireLead(id);
    support.version(entity, input.version());
    if ("CONVERTED".equals(entity.getStatus()) && !"CONVERTED".equals(input.status())
        || !"CONVERTED".equals(entity.getStatus()) && "CONVERTED".equals(input.status()))
      throw ApiException.conflict("A situação de um lead convertido é controlada pela conversão.");
    Long old = entity.getAssigneeId();
    applyLead(entity, input);
    leads.saveAndFlush(entity);
    history.record("LEAD", id, "UPDATE", "Lead atualizado");
    if (!Objects.equals(old, entity.getAssigneeId()))
      support.notifyAssignment(entity.getAssigneeId(), entity.getName(), "/leads");
    return leadView(entity);
  }

  private void applyLead(Lead entity, LeadInput input) {
    support.member(input.assigneeId());
    entity.setName(input.name().strip());
    entity.setEmail(input.email());
    entity.setPhone(input.phone());
    entity.setSource(input.source());
    entity.setStatus(input.status());
    entity.setAssigneeId(input.assigneeId());
    entity.setNotes(input.notes());
  }

  public Map<String, Object> convert(Long id, ConvertInput input) {
    support.current.require("SALES_WRITE");
    support.current.require("CUSTOMERS_WRITE");
    var lead = requireLead(id);
    if ("CONVERTED".equals(lead.getStatus()) || lead.getCustomerId() != null)
      throw ApiException.conflict("Este lead já foi convertido.");
    String name =
        input.name() == null || input.name().isBlank() ? lead.getName() : input.name().strip();
    var customer =
        customers.createEntity(
            new CustomerInput(
                input.type(),
                name,
                input.document(),
                lead.getEmail(),
                lead.getPhone(),
                null,
                true,
                null));
    lead.setCustomerId(customer.getId());
    lead.setStatus("CONVERTED");
    leads.saveAndFlush(lead);
    history.record("LEAD", id, "CONVERT", "Lead convertido em cliente");
    return customers.view(customer);
  }

  public Map<String, Object> leadView(Lead entity) {
    var value = support.view(entity);
    value.put("name", entity.getName());
    value.put("email", entity.getEmail());
    value.put("phone", entity.getPhone());
    value.put("source", entity.getSource());
    value.put("status", entity.getStatus());
    value.put("assigneeId", entity.getAssigneeId());
    value.put("assigneeName", support.memberName(entity.getAssigneeId()));
    value.put("notes", entity.getNotes());
    value.put("customerId", entity.getCustomerId());
    return value;
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> opportunities(
      String search,
      String status,
      Long assigneeId,
      Long customerId,
      LocalDate from,
      LocalDate to,
      Pageable pageable) {
    support.current.require("SALES_READ");
    var spec =
        support.<Opportunity>filter(
            search, List.of("title", "description"), status, "stage", assigneeId, from, to);
    if (customerId != null) {
      customers.require(customerId);
      spec = spec.and((r, q, c) -> c.equal(r.get("customerId"), customerId));
    }
    return PageResponse.from(opportunities.findAll(spec, pageable).map(this::opportunityView));
  }

  @Transactional(readOnly = true)
  public Map<String, Object> opportunityDetail(Long id) {
    support.current.require("SALES_READ");
    return opportunityView(requireOpportunity(id));
  }

  public Map<String, Object> createOpportunity(OpportunityInput input) {
    support.current.require("SALES_WRITE");
    var entity = support.initialize(new Opportunity());
    applyOpportunity(entity, input);
    opportunities.saveAndFlush(entity);
    history.record(
        "OPPORTUNITY", entity.getId(), "CREATE", "Oportunidade cadastrada: " + entity.getStage());
    support.notifyAssignment(entity.getAssigneeId(), entity.getTitle(), "/opportunities");
    return opportunityView(entity);
  }

  public Map<String, Object> updateOpportunity(Long id, OpportunityInput input) {
    support.current.require("SALES_WRITE");
    var entity = requireOpportunity(id);
    support.version(entity, input.version());
    String oldStage = entity.getStage();
    Long oldAssignee = entity.getAssigneeId();
    applyOpportunity(entity, input);
    opportunities.saveAndFlush(entity);
    history.record(
        "OPPORTUNITY",
        id,
        "UPDATE",
        "Oportunidade atualizada; etapa " + oldStage + " → " + entity.getStage());
    if (!Objects.equals(oldAssignee, entity.getAssigneeId()))
      support.notifyAssignment(entity.getAssigneeId(), entity.getTitle(), "/opportunities");
    return opportunityView(entity);
  }

  private void applyOpportunity(Opportunity entity, OpportunityInput input) {
    support.member(input.assigneeId());
    if (input.customerId() != null) customers.require(input.customerId());
    if (input.leadId() != null) requireLead(input.leadId());
    entity.setTitle(input.title().strip());
    entity.setCustomerId(input.customerId());
    entity.setLeadId(input.leadId());
    entity.setAssigneeId(input.assigneeId());
    entity.setAmount(input.amount());
    entity.setProbability(input.probability());
    entity.setExpectedCloseDate(input.expectedCloseDate());
    entity.setDescription(input.description());
    setStage(entity, input.stage());
  }

  private void setStage(Opportunity entity, String stage) {
    entity.setStage(stage);
    if ("WON".equals(stage)) entity.setProbability(100);
    if ("LOST".equals(stage)) entity.setProbability(0);
  }

  public Map<String, Object> stage(Long id, StageInput input) {
    support.current.require("SALES_WRITE");
    var entity = requireOpportunity(id);
    support.version(entity, input.version());
    String previous = entity.getStage();
    setStage(entity, input.stage());
    opportunities.saveAndFlush(entity);
    history.record("OPPORTUNITY", id, "STAGE", previous + " → " + input.stage());
    return opportunityView(entity);
  }

  public Map<String, Object> opportunityView(Opportunity entity) {
    var value = support.view(entity);
    value.put("title", entity.getTitle());
    value.put("customerId", entity.getCustomerId());
    value.put(
        "customerName",
        entity.getCustomerId() == null
            ? null
            : customers.require(entity.getCustomerId()).getName());
    value.put("leadId", entity.getLeadId());
    value.put("assigneeId", entity.getAssigneeId());
    value.put("assigneeName", support.memberName(entity.getAssigneeId()));
    value.put("stage", entity.getStage());
    value.put("amount", entity.getAmount());
    value.put("probability", entity.getProbability());
    value.put("expectedCloseDate", entity.getExpectedCloseDate());
    value.put("description", entity.getDescription());
    return value;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> interactions(Long id) {
    support.current.require("SALES_READ");
    requireOpportunity(id);
    return interactions
        .findByOrganizationIdAndOpportunityIdOrderByCreatedAtDesc(
            support.current.organizationId(), id)
        .stream()
        .map(this::interactionView)
        .toList();
  }

  public Map<String, Object> addInteraction(Long id, NoteInput input) {
    support.current.require("SALES_WRITE");
    requireOpportunity(id);
    var entity = support.initialize(new OpportunityInteraction());
    entity.setOpportunityId(id);
    entity.setAuthorId(support.current.userId());
    entity.setNote(input.note().strip());
    interactions.saveAndFlush(entity);
    history.record("OPPORTUNITY", id, "INTERACTION", "Interação registrada");
    return interactionView(entity);
  }

  private Map<String, Object> interactionView(OpportunityInteraction entity) {
    var value = support.view(entity);
    value.put("note", entity.getNote());
    value.put("authorName", support.memberName(entity.getAuthorId()));
    return value;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> history(Long id) {
    support.current.require("SALES_READ");
    requireOpportunity(id);
    return history.list("OPPORTUNITY", id);
  }
}
