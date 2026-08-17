package com.merchantflow.inventory;
import jakarta.persistence.*;
import java.time.Instant;
@Entity @Table(name = "inventory_transaction") public class InventoryTransaction {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "sku_id", nullable = false) private Long skuId;
  @Column(name = "transaction_type", nullable = false) private String transactionType;
  @Column(name = "quantity_delta", nullable = false) private Integer quantityDelta;
  @Column(name = "before_qty", nullable = false) private Integer beforeQty;
  @Column(name = "after_qty", nullable = false) private Integer afterQty;
  @Column(nullable = false) private String reason;
  @Column(name = "operator_name", nullable = false) private String operatorName;
  @Column(name = "created_at", nullable = false) private Instant createdAt = Instant.now();
  public static InventoryTransaction create(Long skuId, String type, int delta, int before, int after, String reason, String operator) { InventoryTransaction t = new InventoryTransaction(); t.skuId = skuId; t.transactionType = type; t.quantityDelta = delta; t.beforeQty = before; t.afterQty = after; t.reason = reason; t.operatorName = operator; return t; }
  public Long getId() { return id; } public Long getSkuId() { return skuId; } public String getTransactionType() { return transactionType; } public Integer getQuantityDelta() { return quantityDelta; } public Integer getBeforeQty() { return beforeQty; } public Integer getAfterQty() { return afterQty; } public String getReason() { return reason; } public String getOperatorName() { return operatorName; } public Instant getCreatedAt() { return createdAt; }
}
