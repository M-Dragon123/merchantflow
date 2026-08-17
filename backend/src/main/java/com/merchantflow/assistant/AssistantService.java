package com.merchantflow.assistant;

import com.merchantflow.dashboard.DashboardService;
import com.merchantflow.inventory.Inventory;
import com.merchantflow.inventory.InventoryService;
import com.merchantflow.order.OrderStatus;
import com.merchantflow.order.SalesOrder;
import com.merchantflow.order.SalesOrderRepository;
import com.merchantflow.product.ProductSku;
import com.merchantflow.product.ProductSkuRepository;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * AI 运营助手：只读工具集 + 规则路由 + Provider 应答。
 * 安全约束：本服务只查询数据，绝不写库；任何库存调整建议必须由用户在前端二次确认后，
 * 走既有的 POST /api/v1/inventory/adjustments（会记录操作人与原因）。
 */
@Service
public class AssistantService {
  private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("M月d日");

  private final AssistantProvider provider;
  private final InventoryService inventoryService;
  private final DashboardService dashboardService;
  private final SalesOrderRepository orders;
  private final ProductSkuRepository skus;

  public AssistantService(AssistantProvider provider, InventoryService inventoryService,
      DashboardService dashboardService, SalesOrderRepository orders, ProductSkuRepository skus) {
    this.provider = provider;
    this.inventoryService = inventoryService;
    this.dashboardService = dashboardService;
    this.orders = orders;
    this.skus = skus;
  }

  public AssistantDtos.ChatResponse chat(String message) {
    Intent intent = IntentRouter.route(message);
    List<AssistantDtos.ToolResult> results = new ArrayList<>();
    List<AssistantDtos.Suggestion> suggestions = new ArrayList<>();
    switch (intent) {
      case REORDER -> collectReorder(message, results, suggestions);
      case TOP_PRODUCTS -> collectTopProducts(results);
      case SALES -> collectSales(results);
      case PENDING_SHIPMENT -> collectPendingShipment(results);
      case ANOMALIES -> collectAnomalies(results);
      case HELP -> { /* 无需工具 */ }
    }
    String reply = provider.answer(message, intent, results);
    return new AssistantDtos.ChatResponse(reply, suggestions);
  }

  // ---- 只读工具 ----

  private void collectReorder(String message, List<AssistantDtos.ToolResult> results, List<AssistantDtos.Suggestion> suggestions) {
    List<Inventory> alerts = inventoryService.alerts();
    if (alerts.isEmpty()) {
      results.add(new AssistantDtos.ToolResult("low_stock", "当前没有库存低于安全库存的商品，暂无需补货。"));
      return;
    }
    List<String> lines = new ArrayList<>();
    for (Inventory stock : alerts) {
      ProductSku sku = skus.findById(stock.getSkuId()).orElse(null);
      if (sku == null) continue;
      String code = sku.getSkuCode();
      int available = stock.getAvailableQty();
      int safety = stock.getSafetyStock();
      // 建议补齐到安全库存的 2 倍（保守补货策略）
      int suggestQty = Math.max(safety * 2 - available, 1);
      lines.add("· " + code + "：可用 " + available + " / 安全库存 " + safety + "，建议补货 " + suggestQty + " 件");
      suggestions.add(new AssistantDtos.Suggestion("REORDER", stock.getSkuId(), code,
          "补货 " + code, "可用 " + available + " 低于安全库存 " + safety + "，建议入库 " + suggestQty + " 件", suggestQty));
    }
    results.add(new AssistantDtos.ToolResult("low_stock", String.join("\n", lines)));
  }

  private void collectTopProducts(List<AssistantDtos.ToolResult> results) {
    List<DashboardService.TopProduct> top = dashboardService.topProducts(30);
    if (top.isEmpty()) {
      results.add(new AssistantDtos.ToolResult("top_products", "近 30 天暂无已支付订单销量数据。"));
      return;
    }
    List<String> lines = new ArrayList<>();
    for (int i = 0; i < top.size(); i++) {
      DashboardService.TopProduct p = top.get(i);
      lines.add((i + 1) + ". " + p.skuCode() + "：销量 " + p.quantity() + " 件，金额 ¥" + p.amount().setScale(2) + "");
    }
    results.add(new AssistantDtos.ToolResult("top_products", String.join("\n", lines)));
  }

  private void collectSales(List<AssistantDtos.ToolResult> results) {
    DashboardService.Summary s = dashboardService.summary();
    BigDecimal trendTotal = BigDecimal.ZERO;
    for (DashboardService.TrendPoint point : dashboardService.salesTrend(14)) trendTotal = trendTotal.add(point.amount());
    results.add(new AssistantDtos.ToolResult("sales", "今日订单 " + s.todayOrders() + " 笔，今日销售额 ¥" + s.todaySales().setScale(2)
        + "；近 14 天销售额合计 ¥" + trendTotal.setScale(2) + "。"));
  }

  private void collectPendingShipment(List<AssistantDtos.ToolResult> results) {
    long count = dashboardService.summary().pendingShipment();
    List<SalesOrder> recent = orders.findByStatusOrderByIdDesc(OrderStatus.PENDING_SHIPMENT).stream().limit(5).toList();
    StringBuilder sb = new StringBuilder("待发货订单共 " + count + " 笔");
    if (!recent.isEmpty()) {
      sb.append("，最近几笔：");
      sb.append(recent.stream().map(o -> o.getOrderNo() + "(¥" + o.getTotalAmount().setScale(2) + ")").reduce((a, b) -> a + "、" + b).orElse(""));
    }
    results.add(new AssistantDtos.ToolResult("pending_shipment", sb.toString()));
  }

  private void collectAnomalies(List<AssistantDtos.ToolResult> results) {
    DashboardService.Anomalies anomalies = dashboardService.anomalies();
    StringBuilder sb = new StringBuilder();
    if (!anomalies.overduePaymentOrders().isEmpty()) {
      sb.append("待付款超 24 小时未支付 ").append(anomalies.overduePaymentOrders().size()).append(" 笔：");
      sb.append(anomalies.overduePaymentOrders().stream().map(o -> o.orderNo() + "(¥" + o.totalAmount().setScale(2) + "，创建于 " + o.createdAt().toLocalDate().format(DAY) + ")")
          .reduce((a, b) -> a + "、" + b).orElse(""));
    } else {
      sb.append("暂无超时未支付订单");
    }
    if (anomalies.refundingCount() > 0) sb.append("；退款中 ").append(anomalies.refundingCount()).append(" 笔");
    results.add(new AssistantDtos.ToolResult("anomalies", sb.toString()));
  }
}
