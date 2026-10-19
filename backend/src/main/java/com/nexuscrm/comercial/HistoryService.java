package com.nexuscrm.comercial;

import com.nexuscrm.clientes.DomainSupport;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HistoryService {
  private final BusinessHistoryRepository history;
  private final DomainSupport support;

  public HistoryService(BusinessHistoryRepository history, DomainSupport support) {
    this.history = history;
    this.support = support;
  }

  public void record(String type, Long id, String action, String detail) {
    var value = support.initialize(new BusinessHistory());
    value.setEntityType(type);
    value.setEntityId(id);
    value.setAction(action);
    value.setDetail(detail);
    value.setActorId(support.current.userId());
    history.save(value);
    support.audit.record(action, type, id, detail);
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> list(String type, Long id) {
    return history
        .findByOrganizationIdAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            support.current.organizationId(), type, id)
        .stream()
        .map(
            value -> {
              var row = support.view(value);
              row.put("action", value.getAction());
              row.put("detail", value.getDetail());
              row.put("actorName", support.memberName(value.getActorId()));
              return row;
            })
        .toList();
  }
}
