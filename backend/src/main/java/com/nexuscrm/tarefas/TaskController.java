package com.nexuscrm.tarefas;

import com.nexuscrm.tarefas.TaskResponses.*;
import com.nexuscrm.clientes.ResponseMapping;

import com.nexuscrm.clientes.DomainSupport;
import com.nexuscrm.shared.*;
import com.nexuscrm.tarefas.TaskRequests.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {
  private final ResponseMapping responses;
  private final TaskService service;
  private final DomainSupport support;

  public TaskController(TaskService service, DomainSupport support, ResponseMapping responses) {
    this.responses = responses;
    this.service = service;
    this.support = support;
  }

  @GetMapping
  public PageResponse<TaskResponse> list(
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
            page,
            size,
            sort,
            direction,
            Set.of("title", "priority", "status", "dueDate", "createdAt", "updatedAt"))), TaskResponse.class);
  }

  @GetMapping("/{id}")
  public TaskResponse detail(@PathVariable Long id) {
    return responses.one(service.detail(id), TaskResponse.class);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public TaskResponse create(@Valid @RequestBody TaskInput input) {
    return responses.one(service.create(input), TaskResponse.class);
  }

  @PutMapping("/{id}")
  public TaskResponse update(@PathVariable Long id, @Valid @RequestBody TaskInput input) {
    return responses.one(service.update(id, input), TaskResponse.class);
  }

  @PatchMapping("/{id}/status")
  public TaskResponse status(@PathVariable Long id, @Valid @RequestBody StatusInput input) {
    return responses.one(service.status(id, input), TaskResponse.class);
  }
}
