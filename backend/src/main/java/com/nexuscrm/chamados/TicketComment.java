package com.nexuscrm.chamados;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "TICKET_COMMENTS")
public class TicketComment extends TenantEntity {
  @Column(name = "TICKET_ID", nullable = false)
  private Long ticketId;

  @Column(name = "AUTHOR_ID", nullable = false)
  private Long authorId;

  @Column(nullable = false, length = 4000)
  private String body;

  public Long getTicketId() {
    return ticketId;
  }

  public void setTicketId(Long value) {
    this.ticketId = value;
  }

  public Long getAuthorId() {
    return authorId;
  }

  public void setAuthorId(Long value) {
    this.authorId = value;
  }

  public String getBody() {
    return body;
  }

  public void setBody(String value) {
    this.body = value;
  }
}
