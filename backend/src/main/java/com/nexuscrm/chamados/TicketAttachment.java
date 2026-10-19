package com.nexuscrm.chamados;

import com.nexuscrm.shared.TenantEntity;
import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "TICKET_ATTACHMENTS")
public class TicketAttachment extends TenantEntity {
  @Column(name = "TICKET_ID", nullable = false)
  private Long ticketId;

  @Column(name = "AUTHOR_ID", nullable = false)
  private Long authorId;

  @Column(name = "FILE_NAME", nullable = false, length = 200)
  private String fileName;

  @Column(name = "STORAGE_KEY", nullable = false, length = 40)
  private String storageKey;

  @Column(name = "CONTENT_TYPE", nullable = false, length = 100)
  private String contentType;

  @Column(name = "FILE_SIZE", nullable = false)
  private Long size;

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

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String value) {
    this.fileName = value;
  }

  public String getStorageKey() {
    return storageKey;
  }

  public void setStorageKey(String value) {
    this.storageKey = value;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String value) {
    this.contentType = value;
  }

  public Long getSize() {
    return size;
  }

  public void setSize(Long value) {
    this.size = value;
  }
}
