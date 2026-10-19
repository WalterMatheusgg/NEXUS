package com.nexuscrm.relatorios;

import java.time.LocalDate;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
public class ReportController {
  private final ReportService service;

  public ReportController(ReportService service) {
    this.service = service;
  }

  @GetMapping("/{type}")
  public ReportService.Report report(
      @PathVariable String type,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long assigneeId) {
    return service.generate(type, from, to, status, assigneeId);
  }

  @GetMapping("/{type}/export")
  public ResponseEntity<byte[]> export(
      @PathVariable String type,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long assigneeId) {
    var report = service.generate(type, from, to, status, assigneeId);
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment()
                .filename("nexus-" + safeName(type) + ".csv")
                .build()
                .toString())
        .header(HttpHeaders.CACHE_CONTROL, "no-store")
        .header("X-Content-Type-Options", "nosniff")
        .body(service.csv(report));
  }

  private String safeName(String type) {
    return switch (type) {
      case "customers", "opportunities", "tickets", "productivity" -> type;
      default -> "report";
    };
  }
}
