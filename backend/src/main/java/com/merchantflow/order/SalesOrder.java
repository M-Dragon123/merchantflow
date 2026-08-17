package com.merchantflow.order;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Entity @Table(name = "sales_order") public class SalesOrder {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "order_no", nullable = false, unique = true) private String orderNo;
  @Column(name = "customer_id", nullable = false) private Long customerId;
  @Enumerated(EnumType.STRING) @Column(nullable = false) private OrderStatus status;
  @Column(name = "total_amount", nullable = false) private BigDecimal totalAmount;
  @Column(name = "paid_at") private LocalDateTime paidAt;
  @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
  @Version private Long version;
  public Long getId() { return id; } public String getOrderNo() { return orderNo; } public Long getCustomerId() { return customerId; } public OrderStatus getStatus() { return status; } public BigDecimal getTotalAmount() { return totalAmount; } public LocalDateTime getPaidAt() { return paidAt; } public LocalDateTime getCreatedAt() { return createdAt; }
  public static SalesOrder create(String number, Long customerId, BigDecimal total) { SalesOrder o = new SalesOrder(); o.orderNo = number; o.customerId = customerId; o.totalAmount = total; o.status = OrderStatus.PENDING_PAYMENT; return o; }
  public void moveTo(OrderStatus target) { status.requireTransitionTo(target); status = target; }
  public void markPaid(LocalDateTime at) { paidAt = at; }
}
