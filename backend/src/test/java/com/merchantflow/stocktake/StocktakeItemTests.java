package com.merchantflow.stocktake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StocktakeItemTests {
  @Test
  void diffIsCountedMinusSystem() {
    StocktakeItem item = StocktakeItem.create(1L, 10L, 8);
    assertEquals(8, item.getCountedQty());
    assertEquals(0, item.getDiffQty());

    item.updateCounted(5);
    assertEquals(-3, item.getDiffQty());

    item.updateCounted(12);
    assertEquals(4, item.getDiffQty());
  }

  @Test
  void rejectsNegativeCounted() {
    StocktakeItem item = StocktakeItem.create(1L, 10L, 8);
    assertThrows(IllegalArgumentException.class, () -> item.updateCounted(-1));
  }
}
