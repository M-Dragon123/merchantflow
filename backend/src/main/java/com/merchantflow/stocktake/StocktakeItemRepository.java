package com.merchantflow.stocktake;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StocktakeItemRepository extends JpaRepository<StocktakeItem, Long> {
  List<StocktakeItem> findByStocktakeIdOrderByIdAsc(Long stocktakeId);
  Optional<StocktakeItem> findByStocktakeIdAndSkuId(Long stocktakeId, Long skuId);
}
