package com.merchantflow.order;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name = "sales_order_item") public class SalesOrderItem {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "order_id", nullable = false) private Long orderId;
  @Column(name = "sku_id", nullable = false) private Long skuId;
  @Column(name = "sku_name_snapshot", nullable = false) private String skuNameSnapshot;
  @Column(name = "unit_price", nullable = false) private BigDecimal unitPrice;
  @Column(nullable = false) private Integer quantity;
  @Column(name = "subtotal_amount", nullable = false) private BigDecimal subtotalAmount;
  public Long getSkuId() { return skuId; } public Integer getQuantity() { return quantity; } public BigDecimal getUnitPrice() { return unitPrice; } public String getSkuNameSnapshot() { return skuNameSnapshot; } public BigDecimal getSubtotalAmount() { return subtotalAmount; }
  public static SalesOrderItem create(Long orderId, Long skuId, String skuName, BigDecimal price, int qty) { SalesOrderItem item = new SalesOrderItem(); item.orderId = orderId; item.skuId = skuId; item.skuNameSnapshot = skuName; item.unitPrice = price; item.quantity = qty; item.subtotalAmount = price.multiply(BigDecimal.valueOf(qty)); return item; }
}
