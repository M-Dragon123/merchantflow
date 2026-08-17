package com.merchantflow.order;

import java.util.EnumSet;

public enum OrderStatus {
  PENDING_PAYMENT, PENDING_SHIPMENT, SHIPPED, COMPLETED, CANCELLED, REFUNDING, REFUNDED;
  public boolean canTransitionTo(OrderStatus target) {
    return switch (this) {
      case PENDING_PAYMENT -> EnumSet.of(PENDING_SHIPMENT, CANCELLED).contains(target);
      case PENDING_SHIPMENT -> EnumSet.of(SHIPPED, REFUNDING).contains(target);
      case SHIPPED -> EnumSet.of(COMPLETED, REFUNDING).contains(target);
      case REFUNDING -> target == REFUNDED;
      default -> false;
    };
  }
  public void requireTransitionTo(OrderStatus target) { if (!canTransitionTo(target)) throw new IllegalStateException("订单当前状态不允许执行此操作"); }
}
