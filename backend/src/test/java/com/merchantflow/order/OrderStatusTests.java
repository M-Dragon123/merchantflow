package com.merchantflow.order;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
class OrderStatusTests {
  @Test void onlyAllowsDefinedStateTransitions() { assertTrue(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PENDING_SHIPMENT)); assertTrue(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.CANCELLED)); assertFalse(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.SHIPPED)); assertTrue(OrderStatus.PENDING_SHIPMENT.canTransitionTo(OrderStatus.SHIPPED)); assertFalse(OrderStatus.COMPLETED.canTransitionTo(OrderStatus.REFUNDING)); }
}
