package com.merchantflow.inventory;

import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class InventoryAdjustmentRulesTests {
  @Test void rejectsZeroDeltaBeforeTouchingDatabase() {
    InventoryService service = new InventoryService(null, null, null);
    assertThrows(IllegalArgumentException.class, () -> service.adjust(1L, 0, "ADJUSTMENT", "test", "admin"));
  }
}
