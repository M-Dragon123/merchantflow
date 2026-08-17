package com.merchantflow.dashboard;

import com.merchantflow.inventory.InventoryRepository;
import com.merchantflow.order.SalesOrderRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 工作台聚合统计：销售额口径为「已付款且未取消/未退款完成」。 */
@Service
public class DashboardService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private final SalesOrderRepository orders;
  private final InventoryRepository inventory;

  public DashboardService(SalesOrderRepository orders, InventoryRepository inventory) {
    this.orders = orders;
    this.inventory = inventory;
  }

  public Summary summary() {
    LocalDateTime todayStart = LocalDate.now(ZONE).atStartOfDay();
    return new Summary(
        orders.countCreatedSince(todayStart),
        orders.sumPaidSince(todayStart),
        orders.countPendingShipment(),
        inventory.countAlerts());
  }

  public List<TrendPoint> salesTrend(int days) {
    int n = Math.min(Math.max(days, 1), 90);
    LocalDate today = LocalDate.now(ZONE);
    LocalDate from = today.minusDays(n - 1L);
    Map<LocalDate, BigDecimal> byDay = new HashMap<>();
    for (Object[] row : orders.salesTrendSince(from.atStartOfDay())) {
      byDay.put(((java.sql.Date) row[0]).toLocalDate(), (BigDecimal) row[1]);
    }
    return TrendFiller.fill(from, n, byDay);
  }

  public List<TopProduct> topProducts(int days) {
    int n = Math.min(Math.max(days, 1), 365);
    LocalDateTime from = LocalDate.now(ZONE).minusDays(n - 1L).atStartOfDay();
    return orders.topProductsSince(from).stream()
        .map(row -> new TopProduct(
            ((Number) row[0]).longValue(), (String) row[1], ((Number) row[2]).intValue(), (BigDecimal) row[3]))
        .toList();
  }

  public Anomalies anomalies() {
    LocalDateTime cutoff = LocalDateTime.now(ZONE).minusHours(24);
    List<OverdueOrder> overdue = orders.overduePaymentOrders(cutoff).stream()
        .map(row -> new OverdueOrder(
            ((Number) row[0]).longValue(), (String) row[1], (BigDecimal) row[2], ((Timestamp) row[3]).toLocalDateTime()))
        .toList();
    return new Anomalies(overdue, orders.countRefunding());
  }

  public record Summary(long todayOrders, BigDecimal todaySales, long pendingShipment, long lowStock) {}
  public record TrendPoint(LocalDate date, BigDecimal amount) {}
  public record TopProduct(Long skuId, String skuCode, int quantity, BigDecimal amount) {}
  public record OverdueOrder(Long id, String orderNo, BigDecimal totalAmount, LocalDateTime createdAt) {}
  public record Anomalies(List<OverdueOrder> overduePaymentOrders, long refundingCount) {}
}
