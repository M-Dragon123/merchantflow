package com.merchantflow.dashboard;

import com.merchantflow.common.ApiResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 工作台与经营数据（所有登录角色可读）。 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {
  private final DashboardService service;

  public DashboardController(DashboardService service) {
    this.service = service;
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')")
  public ApiResponse<DashboardService.Summary> summary() {
    return ApiResponse.ok(service.summary());
  }

  @GetMapping("/sales-trend")
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')")
  public ApiResponse<List<DashboardService.TrendPoint>> salesTrend(
      @RequestParam(defaultValue = "14") int days) {
    return ApiResponse.ok(service.salesTrend(days));
  }

  @GetMapping("/top-products")
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')")
  public ApiResponse<List<DashboardService.TopProduct>> topProducts(
      @RequestParam(defaultValue = "30") int days) {
    return ApiResponse.ok(service.topProducts(days));
  }

  @GetMapping("/anomalies")
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')")
  public ApiResponse<DashboardService.Anomalies> anomalies() {
    return ApiResponse.ok(service.anomalies());
  }
}
