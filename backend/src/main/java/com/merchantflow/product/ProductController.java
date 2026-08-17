package com.merchantflow.product;

import com.merchantflow.common.ApiResponse;
import com.merchantflow.inventory.Inventory;
import com.merchantflow.inventory.InventoryRepository;
import com.merchantflow.inventory.InventoryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class ProductController {
  private final ProductCategoryRepository categories; private final ProductSpuRepository spus; private final ProductSkuRepository skus; private final InventoryRepository inventory; private final InventoryService inventoryService;
  public ProductController(ProductCategoryRepository categories, ProductSpuRepository spus, ProductSkuRepository skus, InventoryRepository inventory, InventoryService inventoryService) { this.categories = categories; this.spus = spus; this.skus = skus; this.inventory = inventory; this.inventoryService = inventoryService; }
  @GetMapping("/categories") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE')") public ApiResponse<List<CategoryItem>> listCategories() { return ApiResponse.ok(categories.findAllByOrderBySortAscIdAsc().stream().map(c -> new CategoryItem(c.getId(), c.getParentId(), c.getName(), c.getSort())).toList()); }
  @PostMapping("/categories") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") public ApiResponse<CategoryItem> createCategory(@Valid @RequestBody CategoryRequest request) { ProductCategory c = categories.save(ProductCategory.create(request.name(), request.parentId(), request.sort())); return ApiResponse.ok(new CategoryItem(c.getId(), c.getParentId(), c.getName(), c.getSort())); }
  @GetMapping("/products") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE')") public ApiResponse<List<ProductItem>> listProducts() { return ApiResponse.ok(skus.findAllByOrderByIdDesc().stream().map(this::item).toList()); }
  @PostMapping("/products") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") @Transactional public ApiResponse<ProductItem> createProduct(@Valid @RequestBody CreateProductRequest request, Authentication authentication) {
    if (!categories.existsById(request.categoryId())) throw new IllegalArgumentException("商品分类不存在"); if (skus.existsBySkuCode(request.skuCode())) throw new IllegalArgumentException("SKU 编码已存在");
    ProductSpu spu = spus.save(ProductSpu.create(request.categoryId(), request.name())); ProductSku sku = skus.save(ProductSku.create(spu.getId(), request.skuCode(), request.salePrice(), request.costPrice())); inventoryService.createInitialInventory(sku, request.initialQty(), request.safetyStock(), authentication.getName()); return ApiResponse.ok(item(sku));
  }
  @PutMapping("/products/{skuId}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") @Transactional public ApiResponse<ProductItem> updateProduct(@PathVariable Long skuId, @RequestBody UpdateProductRequest request) {
    ProductSku sku = skus.findById(skuId).orElseThrow(() -> new IllegalArgumentException("SKU 不存在"));
    ProductSpu spu = spus.findById(sku.getSpuId()).orElseThrow(() -> new IllegalStateException("SPU 数据不完整"));
    if (request.name() != null && !request.name().isBlank()) spu.rename(request.name());
    sku.updatePrice(request.salePrice(), request.costPrice());
    if (request.status() != null) sku.updateStatus(request.status());
    spus.save(spu); skus.save(sku);
    return ApiResponse.ok(item(sku));
  }
  private ProductItem item(ProductSku sku) { Inventory stock = inventory.findById(sku.getId()).orElse(null); return new ProductItem(sku.getId(), sku.getSpuId(), sku.getSkuCode(), sku.getSalePrice(), sku.getCostPrice(), sku.getStatus(), stock == null ? 0 : stock.getAvailableQty(), stock == null ? 0 : stock.getSafetyStock()); }
  public record CategoryRequest(@NotBlank String name, Long parentId, @Min(0) Integer sort) {} public record CategoryItem(Long id, Long parentId, String name, Integer sort) {}
  public record CreateProductRequest(@NotBlank String name, @NotNull Long categoryId, @NotBlank String skuCode, @NotNull @DecimalMin("0") BigDecimal salePrice, @NotNull @DecimalMin("0") BigDecimal costPrice, @Min(0) int initialQty, @Min(0) int safetyStock) {}
  public record UpdateProductRequest(String name, @DecimalMin("0") BigDecimal salePrice, @DecimalMin("0") BigDecimal costPrice, String status) {}
  public record ProductItem(Long skuId, Long spuId, String skuCode, BigDecimal salePrice, BigDecimal costPrice, String status, int availableQty, int safetyStock) {}
}
