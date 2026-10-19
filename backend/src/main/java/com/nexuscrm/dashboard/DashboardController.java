package com.nexuscrm.dashboard;

import com.nexuscrm.clientes.ResponseMapping;

import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {
  private final ResponseMapping responses;
  private final DashboardService service;

  public DashboardController(DashboardService service, ResponseMapping responses) {
    this.responses = responses;
    this.service = service;
  }

  @GetMapping
  public DashboardResponse dashboard() {
    return responses.one(service.dashboard(), DashboardResponse.class);
  }
}
