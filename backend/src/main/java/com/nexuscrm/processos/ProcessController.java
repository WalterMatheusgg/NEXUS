package com.nexuscrm.processos;

import com.nexuscrm.processos.ProcessResponses.*;
import com.nexuscrm.clientes.ResponseMapping;
import com.nexuscrm.clientes.RelationshipResponses.HistoryResponse;

import com.nexuscrm.clientes.DomainSupport;
import com.nexuscrm.processos.ProcessRequests.*;
import com.nexuscrm.shared.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/processes")
public class ProcessController {
  private final ResponseMapping responses;
  private final ProcessService service;
  private final DomainSupport support;

  public ProcessController(ProcessService service, DomainSupport support, ResponseMapping responses) {
    this.responses = responses;
    this.service = service;
    this.support = support;
  }

  @GetMapping
  public PageResponse<ProcessResponse> list(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long assigneeId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return responses.page(service.list(
        search,
        status,
        assigneeId,
        from,
        to,
        support.pageable(
            page, size, sort, direction, Set.of("title", "status", "createdAt", "updatedAt"))), ProcessResponse.class);
  }

  @GetMapping("/{id}")
  public ProcessResponse detail(@PathVariable Long id) {
    return responses.one(service.detail(id), ProcessResponse.class);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public ProcessResponse create(@Valid @RequestBody ProcessInput input) {
    return responses.one(service.create(input), ProcessResponse.class);
  }

  @PutMapping("/{id}")
  public ProcessResponse update(@PathVariable Long id, @Valid @RequestBody ProcessInput input) {
    return responses.one(service.update(id, input), ProcessResponse.class);
  }

  @GetMapping("/{id}/steps")
  public List<StepResponse> steps(@PathVariable Long id) {
    return responses.list(service.steps(id), StepResponse.class);
  }

  @PostMapping("/{id}/steps")
  @ResponseStatus(HttpStatus.CREATED)
  public StepResponse addStep(@PathVariable Long id, @Valid @RequestBody StepInput input) {
    return responses.one(service.addStep(id, input), StepResponse.class);
  }

  @PatchMapping("/{id}/steps/{stepId}")
  public StepResponse stepStatus(
      @PathVariable Long id, @PathVariable Long stepId, @Valid @RequestBody StepStatusInput input) {
    return responses.one(service.stepStatus(id, stepId, input), StepResponse.class);
  }

  @GetMapping("/{id}/history")
  public List<HistoryResponse> history(@PathVariable Long id) {
    return responses.list(service.history(id), HistoryResponse.class);
  }
}
