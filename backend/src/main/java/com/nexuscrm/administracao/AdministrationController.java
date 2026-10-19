package com.nexuscrm.administracao;

import com.nexuscrm.shared.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
public class AdministrationController {
  private final AdministrationService service;
  private final AuditService audit;
  private final NotificationService notifications;
  private final CurrentUser current;

  public AdministrationController(
      AdministrationService service,
      AuditService audit,
      NotificationService notifications,
      CurrentUser current) {
    this.service = service;
    this.audit = audit;
    this.notifications = notifications;
    this.current = current;
  }

  @GetMapping("/admin/users")
  public PageResponse<AdministrationService.UserView> users(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size,
      @RequestParam(defaultValue = "") String search) {
    return service.users(page, size, search);
  }

  @GetMapping("/admin/members")
  public List<AdministrationService.Member> members() {
    return service.members();
  }

  @PostMapping("/admin/users")
  @ResponseStatus(HttpStatus.CREATED)
  public AdministrationService.UserView createUser(
      @Valid @RequestBody AdministrationService.UserRequest input) {
    return service.saveUser(null, input);
  }

  @PutMapping("/admin/users/{id}")
  public AdministrationService.UserView updateUser(
      @PathVariable Long id, @Valid @RequestBody AdministrationService.UserRequest input) {
    return service.saveUser(id, input);
  }

  @PatchMapping("/admin/users/{id}/password")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void password(
      @PathVariable Long id, @Valid @RequestBody AdministrationService.PasswordRequest input) {
    service.password(id, input.password());
  }

  @GetMapping("/admin/roles")
  public List<AdministrationService.RoleView> roles() {
    return service.roles();
  }

  @GetMapping("/admin/permissions")
  public Set<String> permissions() {
    current.require("ROLES_MANAGE");
    return new TreeSet<>(Permission.all());
  }

  @PostMapping("/admin/roles")
  @ResponseStatus(HttpStatus.CREATED)
  public AdministrationService.RoleView createRole(
      @Valid @RequestBody AdministrationService.RoleRequest input) {
    return service.saveRole(null, input);
  }

  @PutMapping("/admin/roles/{id}")
  public AdministrationService.RoleView updateRole(
      @PathVariable Long id, @Valid @RequestBody AdministrationService.RoleRequest input) {
    return service.saveRole(id, input);
  }

  @GetMapping("/admin/departments")
  public List<AdministrationService.UnitView> departments() {
    return service.departments();
  }

  @GetMapping("/admin/teams")
  public List<AdministrationService.UnitView> teams() {
    return service.teams();
  }

  @PostMapping("/admin/departments")
  public AdministrationService.UnitView createDepartment(
      @Valid @RequestBody AdministrationService.UnitRequest input) {
    return service.saveDepartment(null, input);
  }

  @PutMapping("/admin/departments/{id}")
  public AdministrationService.UnitView department(
      @PathVariable Long id, @Valid @RequestBody AdministrationService.UnitRequest input) {
    return service.saveDepartment(id, input);
  }

  @PostMapping("/admin/teams")
  public AdministrationService.UnitView createTeam(
      @Valid @RequestBody AdministrationService.UnitRequest input) {
    return service.saveTeam(null, input);
  }

  @PutMapping("/admin/teams/{id}")
  public AdministrationService.UnitView team(
      @PathVariable Long id, @Valid @RequestBody AdministrationService.UnitRequest input) {
    return service.saveTeam(id, input);
  }

  @GetMapping("/admin/organization")
  public AdministrationService.OrganizationView organization() {
    return service.organization();
  }

  @PutMapping("/admin/organization")
  public AdministrationService.OrganizationView organization(
      @Valid @RequestBody AdministrationService.OrganizationRequest input) {
    return service.organization(input);
  }

  @GetMapping("/admin/audit")
  public PageResponse<AuditService.AuditView> audit(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return audit.list(page, size);
  }

  @GetMapping("/notifications")
  public List<NotificationService.NotificationView> notifications() {
    return notifications.list();
  }

  @PatchMapping("/notifications/{id}/read")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  public void read(@PathVariable Long id) {
    notifications.read(id);
  }
}
