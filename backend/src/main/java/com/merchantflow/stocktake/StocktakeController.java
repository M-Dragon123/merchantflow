package com.merchantflow.stocktake;

import com.merchantflow.common.ApiResponse;
import com.merchantflow.product.ProductSkuRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/** 库存盘点（管理员 / 仓库员）。 */
@RestController
@RequestMapping("/api/v1/stocktakes")
public class StocktakeController {
  private final StocktakeService service;
  private final ProductSkuRepository skus;

  public StocktakeController(StocktakeService service, ProductSkuRepository skus) {
    this.service = service;
    this.skus = skus;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
  public ApiResponse<List<StocktakeItemView>> list() {
    return ApiResponse.ok(service.list().stream().map(s -> new StocktakeItemView(s.getId(), s.getStocktakeNo(), s.getStatus(), s.getOperatorName(), s.getCreatedAt())).toList());
  }

  @PostMapping
  @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
  public ApiResponse<StocktakeItemView> create(Authentication auth) {
    Stocktake s = service.create(auth.getName());
    return ApiResponse.ok(new StocktakeItemView(s.getId(), s.getStocktakeNo(), s.getStatus(), s.getOperatorName(), s.getCreatedAt()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
  public ApiResponse<StocktakeDetail> detail(@PathVariable Long id) {
    Stocktake s = service.find(id);
    List<Line> lines = service.items(id).stream()
        .map(item -> new Line(item.getSkuId(), skuName(item.getSkuId()), item.getSystemQty(), item.getCountedQty(), item.getDiffQty()))
        .toList();
    return ApiResponse.ok(new StocktakeDetail(new StocktakeItemView(s.getId(), s.getStocktakeNo(), s.getStatus(), s.getOperatorName(), s.getCreatedAt()), lines));
  }

  @PutMapping("/{id}/items/{skuId}")
  @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
  public ApiResponse<Line> updateCounted(@PathVariable Long id, @PathVariable Long skuId, @Valid @RequestBody UpdateCountedRequest request) {
    StocktakeItem item = service.updateCounted(id, skuId, request.countedQty());
    return ApiResponse.ok(new Line(item.getSkuId(), skuName(item.getSkuId()), item.getSystemQty(), item.getCountedQty(), item.getDiffQty()));
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
  public ApiResponse<StocktakeItemView> complete(@PathVariable Long id, Authentication auth) {
    Stocktake s = service.complete(id, auth.getName());
    return ApiResponse.ok(new StocktakeItemView(s.getId(), s.getStocktakeNo(), s.getStatus(), s.getOperatorName(), s.getCreatedAt()));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
  public ApiResponse<StocktakeItemView> cancel(@PathVariable Long id) {
    Stocktake s = service.cancel(id);
    return ApiResponse.ok(new StocktakeItemView(s.getId(), s.getStocktakeNo(), s.getStatus(), s.getOperatorName(), s.getCreatedAt()));
  }

  private String skuName(Long skuId) { return skus.findById(skuId).map(s -> s.getSkuCode()).orElse("SKU-" + skuId); }

  public record UpdateCountedRequest(@NotNull Long skuId, @Min(0) int countedQty) {}
  public record StocktakeItemView(Long id, String stocktakeNo, StocktakeStatus status, String operatorName, java.time.LocalDateTime createdAt) {}
  public record StocktakeDetail(StocktakeItemView stocktake, List<Line> items) {}
  public record Line(Long skuId, String skuCode, int systemQty, int countedQty, int diffQty) {}
}
