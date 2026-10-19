package com.nexuscrm.clientes;

import com.nexuscrm.clientes.CustomerResponses.*;

import com.nexuscrm.clientes.CustomerRequests.*;
import com.nexuscrm.shared.*;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/customers")
public class CustomerController {
  private final ResponseMapping responses;
  private final CustomerService service;
  private final DomainSupport support;

  public CustomerController(CustomerService service, DomainSupport support, ResponseMapping responses) {
    this.responses = responses;
    this.service = service;
    this.support = support;
  }

  @GetMapping
  public PageResponse<CustomerResponse> list(
      @RequestParam(defaultValue = "") String search,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {
    return responses.page(service.list(
        search,
        status,
        from,
        to,
        support.pageable(
            page, size, sort, direction, Set.of("name", "document", "createdAt", "updatedAt"))), CustomerResponse.class);
  }

  @GetMapping("/{id}")
  public CustomerResponse detail(@PathVariable Long id) {
    return responses.one(service.detail(id), CustomerResponse.class);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerResponse create(@Valid @RequestBody CustomerInput input) {
    return responses.one(service.create(input), CustomerResponse.class);
  }

  @PutMapping("/{id}")
  public CustomerResponse update(
      @PathVariable Long id, @Valid @RequestBody CustomerInput input) {
    return responses.one(service.update(id, input), CustomerResponse.class);
  }

  @GetMapping("/{id}/contacts")
  public List<ContactResponse> contacts(@PathVariable Long id) {
    return responses.list(service.contactList(id), ContactResponse.class);
  }

  @PostMapping("/{id}/contacts")
  @ResponseStatus(HttpStatus.CREATED)
  public ContactResponse contact(
      @PathVariable Long id, @Valid @RequestBody ContactInput input) {
    return responses.one(service.addContact(id, input), ContactResponse.class);
  }

  @DeleteMapping("/{id}/contacts/{contactId}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void deleteContact(@PathVariable Long id, @PathVariable Long contactId) {
    service.removeContact(id, contactId);
  }

  @GetMapping("/{id}/interactions")
  public List<CustomerInteractionResponse> interactions(@PathVariable Long id) {
    return responses.list(service.interactionList(id), CustomerInteractionResponse.class);
  }

  @PostMapping("/{id}/interactions")
  @ResponseStatus(HttpStatus.CREATED)
  public CustomerInteractionResponse interaction(
      @PathVariable Long id, @Valid @RequestBody NoteInput input) {
    return responses.one(service.addInteraction(id, input), CustomerInteractionResponse.class);
  }
}
