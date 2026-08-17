package com.merchantflow.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** 销售趋势按天补零：缺失日期补 0，保证图表连续。 */
public final class TrendFiller {
  private TrendFiller() {}

  public static List<DashboardService.TrendPoint> fill(
      LocalDate from, int days, Map<LocalDate, BigDecimal> byDay) {
    Map<LocalDate, BigDecimal> source = byDay == null ? Map.of() : byDay;
    List<DashboardService.TrendPoint> points = new ArrayList<>(days);
    for (int i = 0; i < days; i++) {
      LocalDate day = from.plusDays(i);
      points.add(new DashboardService.TrendPoint(day, source.getOrDefault(day, BigDecimal.ZERO)));
    }
    return points;
  }
}
