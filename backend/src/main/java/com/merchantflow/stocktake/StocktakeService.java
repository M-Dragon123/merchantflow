package com.merchantflow.stocktake;

import com.merchantflow.inventory.Inventory;
import com.merchantflow.inventory.InventoryRepository;
import com.merchantflow.inventory.InventoryTransaction;
import com.merchantflow.inventory.InventoryTransactionRepository;
import com.merchantflow.product.ProductSku;
import com.merchantflow.product.ProductSkuRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;

/** 盘点：草稿单自动带入全部 SKU 账面数，完成时差异统一写入库存流水（ADJUSTMENT）。 */
@Service
public class StocktakeService {
  private final StocktakeRepository stocktakes;
  private final StocktakeItemRepository items;
  private final ProductSkuRepository skus;
  private final InventoryRepository inventory;
  private final InventoryTransactionRepository transactions;

  public StocktakeService(StocktakeRepository stocktakes, StocktakeItemRepository items,
      ProductSkuRepository skus, InventoryRepository inventory, InventoryTransactionRepository transactions) {
    this.stocktakes = stocktakes;
    this.items = items;
    this.skus = skus;
    this.inventory = inventory;
    this.transactions = transactions;
  }

  @Transactional
  public Stocktake create(String operator) {
    List<ProductSku> all = skus.findAllByOrderByIdDesc();
    if (all.isEmpty()) throw new IllegalArgumentException("暂无商品，无法发起盘点");
    Stocktake stocktake = stocktakes.save(Stocktake.create("PD" + System.currentTimeMillis(), operator));
    for (ProductSku sku : all) {
      Inventory inv = inventory.findById(sku.getId()).orElse(null);
      items.save(StocktakeItem.create(stocktake.getId(), sku.getId(), inv == null ? 0 : inv.getAvailableQty()));
    }
    return stocktake;
  }

  public List<Stocktake> list() { return stocktakes.findAllByOrderByIdDesc(); }

  public Stocktake find(Long id) { return stocktakes.findById(id).orElseThrow(() -> new IllegalArgumentException("盘点单不存在")); }

  public List<StocktakeItem> items(Long id) { return items.findByStocktakeIdOrderByIdAsc(id); }

  @Transactional
  public StocktakeItem updateCounted(Long stocktakeId, Long skuId, int countedQty) {
    Stocktake stocktake = requireDraft(stocktakeId);
    StocktakeItem item = items.findByStocktakeIdAndSkuId(stocktakeId, skuId).orElseThrow(() -> new IllegalArgumentException("盘点明细不存在"));
    item.updateCounted(countedQty);
    return items.save(item);
  }

  @Transactional
  public Stocktake complete(Long id, String operator) {
    Stocktake stocktake = requireDraft(id);
    for (StocktakeItem item : items.findByStocktakeIdOrderByIdAsc(id)) {
      if (item.getDiffQty() == 0) continue;
      // 差异为正即盘盈（入库），为负即盘亏（出库）；原子更新保证不会把库存扣成负数
      if (inventory.applyDelta(item.getSkuId(), item.getDiffQty()) != 1)
        throw new IllegalStateException("库存不足或发生并发冲突，盘点完成失败（SKU " + item.getSkuId() + "）");
      Inventory current = inventory.findById(item.getSkuId()).orElseThrow(() -> new IllegalStateException("库存记录不存在"));
      int after = current.getAvailableQty();
      transactions.save(InventoryTransaction.create(item.getSkuId(), "ADJUSTMENT", item.getDiffQty(), after - item.getDiffQty(), after, "盘点差异调整：" + stocktake.getStocktakeNo(), operator));
    }
    stocktake.markCompleted();
    return stocktakes.save(stocktake);
  }

  @Transactional
  public Stocktake cancel(Long id) {
    Stocktake stocktake = requireDraft(id);
    stocktake.markCancelled();
    return stocktakes.save(stocktake);
  }

  private Stocktake requireDraft(Long id) {
    Stocktake stocktake = find(id);
    if (stocktake.getStatus() != StocktakeStatus.DRAFT) throw new IllegalStateException("仅草稿状态的盘点单可执行此操作");
    return stocktake;
  }
}
