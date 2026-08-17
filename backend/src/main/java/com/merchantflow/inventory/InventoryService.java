package com.merchantflow.inventory;
import com.merchantflow.product.ProductSku;
import com.merchantflow.product.ProductSkuRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import org.springframework.stereotype.Service;
@Service public class InventoryService {
  private final InventoryRepository inventory; private final InventoryTransactionRepository transactions; private final ProductSkuRepository skus;
  public InventoryService(InventoryRepository inventory, InventoryTransactionRepository transactions, ProductSkuRepository skus) { this.inventory = inventory; this.transactions = transactions; this.skus = skus; }
  @Transactional public TransactionResult adjust(Long skuId, int delta, String type, String reason, String operator) {
    if (delta == 0) throw new IllegalArgumentException("调整数量不能为 0"); if (!List.of("INBOUND", "OUTBOUND", "ADJUSTMENT").contains(type)) throw new IllegalArgumentException("不支持的库存流水类型");
    if ((type.equals("INBOUND") && delta < 0) || (type.equals("OUTBOUND") && delta > 0)) throw new IllegalArgumentException("库存类型与调整方向不匹配");
    if (inventory.applyDelta(skuId, delta) != 1) throw new IllegalStateException("库存不足、商品不存在或发生并发冲突");
    Inventory current = inventory.findById(skuId).orElseThrow(() -> new IllegalStateException("库存记录不存在")); int after = current.getAvailableQty(); int before = after - delta;
    transactions.save(InventoryTransaction.create(skuId, type, delta, before, after, reason == null || reason.isBlank() ? "人工调整" : reason, operator));
    return new TransactionResult(skuId, before, after, current.getVersion());
  }
  @Transactional public Long createInitialInventory(ProductSku sku, int quantity, int safetyStock, String operator) { if (quantity < 0 || safetyStock < 0) throw new IllegalArgumentException("初始库存与安全库存不能小于 0"); inventory.save(Inventory.create(sku.getId(), quantity, safetyStock)); if (quantity > 0) transactions.save(InventoryTransaction.create(sku.getId(), "INBOUND", quantity, 0, quantity, "商品建档初始库存", operator)); return sku.getId(); }
  public List<Inventory> alerts() { return inventory.findAlerts(); } public List<InventoryTransaction> transactions() { return transactions.findTop100ByOrderByCreatedAtDesc(); } public Inventory get(Long skuId) { return inventory.findById(skuId).orElseThrow(() -> new IllegalArgumentException("SKU 库存不存在")); }
  public record TransactionResult(Long skuId, int beforeQty, int afterQty, Long version) {}
}
