package com.merchantflow.order;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "order_operation_log") public class OrderOperationLog {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "order_id", nullable = false) private Long orderId;
  @Column(nullable = false) private String action;
  @Column(name = "from_status") private String fromStatus;
  @Column(name = "to_status", nullable = false) private String toStatus;
  @Column(nullable = false) private String remark;
  @Column(name = "operator_name", nullable = false) private String operatorName;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
  public static OrderOperationLog create(Long id, String action, OrderStatus from, OrderStatus to, String remark, String operator) { OrderOperationLog log = new OrderOperationLog(); log.orderId = id; log.action = action; log.fromStatus = from == null ? null : from.name(); log.toStatus = to.name(); log.remark = remark == null || remark.isBlank() ? "" : remark; log.operatorName = operator; return log; }
  public Long getId() { return id; } public Long getOrderId() { return orderId; } public String getAction() { return action; } public String getFromStatus() { return fromStatus; } public String getToStatus() { return toStatus; } public String getRemark() { return remark; } public String getOperatorName() { return operatorName; } public Instant getCreatedAt() { return createdAt; }
}
