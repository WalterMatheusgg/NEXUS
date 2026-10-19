package com.nexuscrm.clientes;

import com.nexuscrm.clientes.CustomerRequests.*;
import com.nexuscrm.shared.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CustomerService {
  private final CustomerRepository customers;
  private final CustomerContactRepository contacts;
  private final CustomerInteractionRepository interactions;
  private final DomainSupport support;

  public CustomerService(
      CustomerRepository customers,
      CustomerContactRepository contacts,
      CustomerInteractionRepository interactions,
      DomainSupport support) {
    this.customers = customers;
    this.contacts = contacts;
    this.interactions = interactions;
    this.support = support;
  }

  @Transactional(readOnly = true)
  public PageResponse<Map<String, Object>> list(
      String search, String status, LocalDate from, LocalDate to, Pageable pageable) {
    support.current.require("CUSTOMERS_READ");
    Boolean active = null;
    if (status != null && !status.isBlank()) {
      if (!Set.of("ACTIVE", "INACTIVE").contains(status))
        throw ApiException.badRequest("Situação de cliente inválida.");
      active = "ACTIVE".equals(status);
    }
    var spec =
        support.<Customer>filter(
            search, List.of("name", "document", "email"), null, null, null, from, to);
    if (active != null) {
      boolean flag = active;
      spec = spec.and((r, q, c) -> c.equal(r.get("active"), flag));
    }
    return PageResponse.from(customers.findAll(spec, pageable).map(this::view));
  }

  public Customer require(Long id) {
    return support.get(customers, id, "Cliente");
  }

  @Transactional(readOnly = true)
  public Map<String, Object> detail(Long id) {
    support.current.require("CUSTOMERS_READ");
    var value = view(require(id));
    value.put("contacts", contactList(id));
    value.put("interactions", interactionList(id));
    return value;
  }

  public Map<String, Object> create(CustomerInput input) {
    support.current.require("CUSTOMERS_WRITE");
    return view(createEntity(input));
  }

  public Customer createEntity(CustomerInput input) {
    String document = BrazilianDocument.normalize(input.type(), input.document());
    if (customers.existsByOrganizationIdAndDocument(support.current.organizationId(), document))
      throw ApiException.conflict("Já existe cliente com esse CPF/CNPJ nesta organização.");
    var entity = support.initialize(new Customer());
    apply(entity, input, document);
    customers.saveAndFlush(entity);
    support.audit.record("CREATE", "CUSTOMER", entity.getId(), "Cliente cadastrado");
    return entity;
  }

  public Map<String, Object> update(Long id, CustomerInput input) {
    support.current.require("CUSTOMERS_WRITE");
    var entity = require(id);
    support.version(entity, input.version());
    String document = BrazilianDocument.normalize(input.type(), input.document());
    if (customers.existsByOrganizationIdAndDocumentAndIdNot(
        support.current.organizationId(), document, id))
      throw ApiException.conflict("Já existe cliente com esse CPF/CNPJ nesta organização.");
    apply(entity, input, document);
    customers.saveAndFlush(entity);
    support.audit.record("UPDATE", "CUSTOMER", id, "Cliente atualizado");
    return view(entity);
  }

  private void apply(Customer entity, CustomerInput input, String document) {
    entity.setType(input.type());
    entity.setName(input.name().strip());
    entity.setDocument(document);
    entity.setEmail(input.email());
    entity.setPhone(input.phone());
    entity.setAddress(input.address());
    entity.setActive(input.active());
  }

  public Map<String, Object> view(Customer entity) {
    var value = support.view(entity);
    value.put("type", entity.getType());
    value.put("name", entity.getName());
    value.put("document", entity.getDocument());
    value.put("email", entity.getEmail());
    value.put("phone", entity.getPhone());
    value.put("address", entity.getAddress());
    value.put("active", entity.getActive());
    return value;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> contactList(Long id) {
    support.current.require("CUSTOMERS_READ");
    require(id);
    return contacts
        .findByOrganizationIdAndCustomerIdOrderByName(support.current.organizationId(), id)
        .stream()
        .map(this::contactView)
        .toList();
  }

  public Map<String, Object> addContact(Long id, ContactInput input) {
    support.current.require("CUSTOMERS_WRITE");
    require(id);
    var entity = support.initialize(new CustomerContact());
    entity.setCustomerId(id);
    entity.setName(input.name().strip());
    entity.setEmail(input.email());
    entity.setPhone(input.phone());
    entity.setPosition(input.position());
    contacts.saveAndFlush(entity);
    support.audit.record("CREATE", "CUSTOMER_CONTACT", entity.getId(), "Contato cadastrado");
    return contactView(entity);
  }

  public void removeContact(Long id, Long contactId) {
    support.current.require("CUSTOMERS_WRITE");
    require(id);
    var contact =
        contacts
            .findByIdAndOrganizationIdAndCustomerId(contactId, support.current.organizationId(), id)
            .orElseThrow(() -> ApiException.notFound("Contato não encontrado."));
    contacts.delete(contact);
    support.audit.record("DELETE", "CUSTOMER_CONTACT", contactId, "Contato removido");
  }

  private Map<String, Object> contactView(CustomerContact entity) {
    var value = support.view(entity);
    value.put("name", entity.getName());
    value.put("email", entity.getEmail());
    value.put("phone", entity.getPhone());
    value.put("position", entity.getPosition());
    return value;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> interactionList(Long id) {
    support.current.require("CUSTOMERS_READ");
    require(id);
    return interactions
        .findByOrganizationIdAndCustomerIdOrderByCreatedAtDesc(support.current.organizationId(), id)
        .stream()
        .map(this::interactionView)
        .toList();
  }

  public Map<String, Object> addInteraction(Long id, NoteInput input) {
    support.current.require("CUSTOMERS_WRITE");
    require(id);
    var entity = support.initialize(new CustomerInteraction());
    entity.setCustomerId(id);
    entity.setAuthorId(support.current.userId());
    entity.setNote(input.note().strip());
    interactions.saveAndFlush(entity);
    support.audit.record("INTERACTION", "CUSTOMER", id, "Interação registrada");
    return interactionView(entity);
  }

  private Map<String, Object> interactionView(CustomerInteraction entity) {
    var value = support.view(entity);
    value.put("note", entity.getNote());
    value.put("authorName", support.memberName(entity.getAuthorId()));
    return value;
  }
}
