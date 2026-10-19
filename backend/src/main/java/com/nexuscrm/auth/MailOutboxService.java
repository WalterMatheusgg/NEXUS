package com.nexuscrm.auth;

import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MailOutboxService {
  private static final Logger LOG = LoggerFactory.getLogger(MailOutboxService.class);
  private final MailOutboxRepository repository;
  private final JavaMailSender sender;
  private final String from;

  public MailOutboxService(
      MailOutboxRepository repository,
      JavaMailSender sender,
      @Value("${nexus.mail.from}") String from) {
    this.repository = repository;
    this.sender = sender;
    this.from = from;
  }

  @Transactional
  public void queue(Long org, String recipient, String subject, String body) {
    MailOutbox mail = new MailOutbox();
    mail.setOrganizationId(org);
    mail.recipient = recipient;
    mail.subject = subject;
    mail.body = body;
    repository.save(mail);
  }

  @Scheduled(fixedDelayString = "${nexus.mail.poll-ms:10000}")
  @Transactional
  public void deliver() {
    for (MailOutbox mail : repository.pending(OffsetDateTime.now(), PageRequest.of(0, 10))) {
      try {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(mail.recipient);
        message.setSubject(mail.subject);
        message.setText(mail.body);
        sender.send(message);
        mail.sentAt = OffsetDateTime.now();
        mail.body = "[delivered]";
      } catch (org.springframework.mail.MailException failure) {
        mail.attempts++;
        mail.nextAttemptAt = OffsetDateTime.now().plusSeconds(Math.min(300, 30L * mail.attempts));
        LOG.warn("Mail delivery retry queued id={} attempt={}", mail.getId(), mail.attempts);
      }
    }
  }
}
