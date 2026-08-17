package com.merchantflow.product;
import jakarta.persistence.*;
import java.math.BigDecimal;
@Entity @Table(name = "product_sku") public class ProductSku {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "spu_id", nullable = false) private Long spuId;
  @Column(name = "sku_code", nullable = false, unique = true) private String skuCode;
  @Column(name = "specs_json") private String specsJson;
  @Column(name = "sale_price", nullable = false) private BigDecimal salePrice;
  @Column(name = "cost_price", nullable = false) private BigDecimal costPrice;
  @Column(nullable = false) private String status = "ACTIVE";
  public Long getId() { return id; } public Long getSpuId() { return spuId; } public String getSkuCode() { return skuCode; } public BigDecimal getSalePrice() { return salePrice; } public BigDecimal getCostPrice() { return costPrice; } public String getStatus() { return status; }
  public static ProductSku create(Long spuId, String code, BigDecimal sale, BigDecimal cost) { ProductSku sku = new ProductSku(); sku.spuId = spuId; sku.skuCode = code; sku.salePrice = sale; sku.costPrice = cost; sku.specsJson = "{}"; return sku; }
  public void updatePrice(BigDecimal sale, BigDecimal cost) { if (sale != null) salePrice = sale; if (cost != null) costPrice = cost; }
  public void updateStatus(String newStatus) { if (!"ACTIVE".equals(newStatus) && !"INACTIVE".equals(newStatus)) throw new IllegalArgumentException("状态仅支持 ACTIVE / INACTIVE"); status = newStatus; }
}
