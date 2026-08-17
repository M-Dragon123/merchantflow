package com.merchantflow.inventory;
import jakarta.persistence.*;
@Entity @Table(name = "inventory") public class Inventory {
  @Id @Column(name = "sku_id") private Long skuId;
  @Column(name = "available_qty", nullable = false) private Integer availableQty;
  @Column(name = "locked_qty", nullable = false) private Integer lockedQty;
  @Column(name = "safety_stock", nullable = false) private Integer safetyStock;
  @Version private Long version;
  public Long getSkuId() { return skuId; } public Integer getAvailableQty() { return availableQty; } public Integer getLockedQty() { return lockedQty; } public Integer getSafetyStock() { return safetyStock; } public Long getVersion() { return version; }
  /** 注意：version 不能手动赋值，Hibernate 要求新实体 @Version 为 null（插入时自动初始化）。 */
  public static Inventory create(Long skuId, int quantity, int safetyStock) { Inventory i = new Inventory(); i.skuId = skuId; i.availableQty = quantity; i.lockedQty = 0; i.safetyStock = safetyStock; return i; }
}
