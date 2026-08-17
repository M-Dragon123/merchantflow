package com.merchantflow.stocktake;
import jakarta.persistence.*;
@Entity @Table(name = "stocktake_item") public class StocktakeItem {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "stocktake_id", nullable = false) private Long stocktakeId;
  @Column(name = "sku_id", nullable = false) private Long skuId;
  @Column(name = "system_qty", nullable = false) private int systemQty;
  @Column(name = "counted_qty", nullable = false) private int countedQty;
  @Column(name = "diff_qty", nullable = false) private int diffQty;
  public Long getId() { return id; } public Long getStocktakeId() { return stocktakeId; } public Long getSkuId() { return skuId; } public int getSystemQty() { return systemQty; } public int getCountedQty() { return countedQty; } public int getDiffQty() { return diffQty; }
  public static StocktakeItem create(Long stocktakeId, Long skuId, int systemQty) { StocktakeItem item = new StocktakeItem(); item.stocktakeId = stocktakeId; item.skuId = skuId; item.systemQty = systemQty; item.countedQty = systemQty; item.diffQty = 0; return item; }
  /** 录入实盘数：不能为负，差异 = 实盘 - 账面。 */
  public void updateCounted(int counted) { if (counted < 0) throw new IllegalArgumentException("实盘数量不能为负数"); countedQty = counted; diffQty = counted - systemQty; }
}
