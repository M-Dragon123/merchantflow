package com.merchantflow.inventory;
import java.util.List;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
public interface InventoryRepository extends JpaRepository<Inventory, Long> {
  @Modifying @Query("update Inventory i set i.availableQty = i.availableQty + :delta, i.version = i.version + 1 where i.skuId = :skuId and i.availableQty + :delta >= 0")
  int applyDelta(@Param("skuId") Long skuId, @Param("delta") int delta);
  @Modifying @Query("update Inventory i set i.lockedQty = i.lockedQty + :quantity, i.version = i.version + 1 where i.skuId = :skuId and i.availableQty - i.lockedQty >= :quantity")
  int reserve(@Param("skuId") Long skuId, @Param("quantity") int quantity);
  @Modifying @Query("update Inventory i set i.lockedQty = i.lockedQty - :quantity, i.version = i.version + 1 where i.skuId = :skuId and i.lockedQty >= :quantity")
  int release(@Param("skuId") Long skuId, @Param("quantity") int quantity);
  @Modifying @Query("update Inventory i set i.availableQty = i.availableQty - :quantity, i.lockedQty = i.lockedQty - :quantity, i.version = i.version + 1 where i.skuId = :skuId and i.availableQty >= :quantity and i.lockedQty >= :quantity")
  int consumeReservation(@Param("skuId") Long skuId, @Param("quantity") int quantity);
  @Query("select i from Inventory i where i.availableQty <= i.safetyStock order by i.availableQty asc") List<Inventory> findAlerts();
  @Query("select count(i) from Inventory i where i.availableQty <= i.safetyStock") long countAlerts();
}
