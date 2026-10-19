package com.nexuscrm.comercial;

import com.nexuscrm.comercial.SalesResponses.*;
import com.nexuscrm.clientes.CustomerResponses.CustomerResponse;
import com.nexuscrm.clientes.RelationshipResponses.HistoryResponse;

import com.nexuscrm.clientes.*;
import com.nexuscrm.clientes.CustomerRequests.NoteInput;
import com.nexuscrm.comercial.SalesRequests.*;
import com.nexuscrm.shared.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class SalesController {
  private final ResponseMapping responses;
  private final SalesService service;
  private final DomainSupport support;

  public SalesController(SalesService service, DomainSupport support, ResponseMapping responses) {
    this.responses = responses;
    this.service = service;
    this.support = support;
  }

  @GetMapping("/leads")
  public PageResponse<LeadResponse> leads(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long assigneeId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return responses.page(service.leads(
        search,
        status,
        assigneeId,
        from,
        to,
        support.pageable(
            page, size, sort, direction, Set.of("name", "status", "createdAt", "updatedAt"))), LeadResponse.class);
  }

  @GetMapping("/leads/{id}")
  public LeadResponse lead(@PathVariable Long id) {
    return responses.one(service.leadDetail(id), LeadResponse.class);
  }

  @PostMapping("/leads")
  @ResponseStatus(HttpStatus.CREATED)
  public LeadResponse createLead(@Valid @RequestBody LeadInput input) {
    return responses.one(service.createLead(input), LeadResponse.class);
  }

  @PutMapping("/leads/{id}")
  public LeadResponse updateLead(
      @PathVariable Long id, @Valid @RequestBody LeadInput input) {
    return responses.one(service.updateLead(id, input), LeadResponse.class);
  }

  @PostMapping("/leads/{id}/convert")
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerResponse convert(
      @PathVariable Long id, @Valid @RequestBody ConvertInput input) {
    return responses.one(service.convert(id, input), CustomerResponse.class);
  }

  @GetMapping("/opportunities")
  public PageResponse<OpportunityResponse> opportunities(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long assigneeId,
      @RequestParam(required = false) Long customerId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return responses.page(service.opportunities(
        search,
        status,
        assigneeId,
        customerId,
        from,
        to,
        support.pageable(
            page,
            size,
            sort,
            direction,
            Set.of("title", "stage", "amount", "expectedCloseDate", "createdAt", "updatedAt"))), OpportunityResponse.class);
  }

  @GetMapping("/opportunities/{id}")
  public OpportunityResponse opportunity(@PathVariable Long id) {
    return responses.one(service.opportunityDetail(id), OpportunityResponse.class);
  }

  @PostMapping("/opportunities")
  @ResponseStatus(HttpStatus.CREATED)
  public OpportunityResponse createOpportunity(@Valid @RequestBody OpportunityInput input) {
    return responses.one(service.createOpportunity(input), OpportunityResponse.class);
  }

  @PutMapping("/opportunities/{id}")
  public OpportunityResponse updateOpportunity(
      @PathVariable Long id, @Valid @RequestBody OpportunityInput input) {
    return responses.one(service.updateOpportunity(id, input), OpportunityResponse.class);
  }

  @PatchMapping("/opportunities/{id}/stage")
  public OpportunityResponse stage(@PathVariable Long id, @Valid @RequestBody StageInput input) {
    return responses.one(service.stage(id, input), OpportunityResponse.class);
  }

  @GetMapping("/opportunities/{id}/interactions")
  public List<OpportunityInteractionResponse> interactions(@PathVariable Long id) {
    return responses.list(service.interactions(id), OpportunityInteractionResponse.class);
  }

  @PostMapping("/opportunities/{id}/interactions")
  @ResponseStatus(HttpStatus.CREATED)
  public OpportunityInteractionResponse interaction(
      @PathVariable Long id, @Valid @RequestBody NoteInput input) {
    return responses.one(service.addInteraction(id, input), OpportunityInteractionResponse.class);
  }

  @GetMapping("/opportunities/{id}/history")
  public List<HistoryResponse> history(@PathVariable Long id) {
    return responses.list(service.history(id), HistoryResponse.class);
  }
}
