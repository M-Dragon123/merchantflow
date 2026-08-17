package com.merchantflow.inventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> { List<InventoryTransaction> findTop100ByOrderByCreatedAtDesc(); }
