package com.merchantflow.inventory;

import com.merchantflow.common.ApiResponse;
import com.merchantflow.product.ProductSku;
import com.merchantflow.product.ProductSkuRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/inventory") public class InventoryController {
  private final InventoryRepository inventory; private final InventoryService service; private final ProductSkuRepository skus;
  public InventoryController(InventoryRepository inventory, InventoryService service, ProductSkuRepository skus) { this.inventory = inventory; this.service = service; this.skus = skus; }
  @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE')") public ApiResponse<List<StockItem>> list() { return ApiResponse.ok(inventory.findAll().stream().map(this::stock).toList()); }
  @GetMapping("/alerts") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE')") public ApiResponse<List<StockItem>> alerts() { return ApiResponse.ok(service.alerts().stream().map(this::stock).toList()); }
  @GetMapping("/transactions") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE')") public ApiResponse<List<TransactionItem>> transactions() { return ApiResponse.ok(service.transactions().stream().map(t -> new TransactionItem(t.getId(), t.getSkuId(), t.getTransactionType(), t.getQuantityDelta(), t.getBeforeQty(), t.getAfterQty(), t.getReason(), t.getOperatorName(), t.getCreatedAt())).toList()); }
  @PostMapping("/adjustments") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") public ApiResponse<InventoryService.TransactionResult> adjust(@Valid @RequestBody AdjustmentRequest request, Authentication auth) { return ApiResponse.ok(service.adjust(request.skuId(), request.delta(), request.type(), request.reason(), auth.getName())); }
  private StockItem stock(Inventory i) { ProductSku sku = skus.findById(i.getSkuId()).orElseThrow(() -> new IllegalStateException("SKU 数据不完整")); return new StockItem(i.getSkuId(), sku.getSkuCode(), i.getAvailableQty(), i.getLockedQty(), i.getSafetyStock(), i.getVersion()); }
  public record AdjustmentRequest(@NotNull Long skuId, int delta, @NotNull String type, String reason) {} public record StockItem(Long skuId, String skuCode, int availableQty, int lockedQty, int safetyStock, Long version) {} public record TransactionItem(Long id, Long skuId, String type, int delta, int beforeQty, int afterQty, String reason, String operator, Instant createdAt) {}
}
