package com.merchantflow.dashboard;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TrendFillerTests {
  @Test
  void fillsMissingDaysWithZero() {
    LocalDate from = LocalDate.of(2026, 1, 1);
    Map<LocalDate, BigDecimal> byDay = new HashMap<>();
    byDay.put(from.plusDays(1), new BigDecimal("100.50"));

    List<DashboardService.TrendPoint> points = TrendFiller.fill(from, 3, byDay);

    assertEquals(3, points.size());
    assertEquals(from, points.get(0).date());
    assertEquals(BigDecimal.ZERO, points.get(0).amount());
    assertEquals(new BigDecimal("100.50"), points.get(1).amount());
    assertEquals(BigDecimal.ZERO, points.get(2).amount());
  }

  @Test
  void acceptsNullMapAsAllZero() {
    List<DashboardService.TrendPoint> points = TrendFiller.fill(LocalDate.of(2026, 2, 1), 2, null);
    assertEquals(2, points.size());
    assertEquals(BigDecimal.ZERO, points.get(0).amount());
    assertEquals(BigDecimal.ZERO, points.get(1).amount());
  }
}
