package com.merchantflow.order;

import com.merchantflow.inventory.Inventory;
import com.merchantflow.inventory.InventoryRepository;
import com.merchantflow.inventory.InventoryTransaction;
import com.merchantflow.inventory.InventoryTransactionRepository;
import com.merchantflow.product.ProductSku;
import com.merchantflow.product.ProductSkuRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service public class OrderService {
  private static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private final CustomerRepository customers; private final SalesOrderRepository orders; private final SalesOrderItemRepository items; private final OrderOperationLogRepository logs; private final ProductSkuRepository skus; private final InventoryRepository inventory; private final InventoryTransactionRepository inventoryTransactions;
  public OrderService(CustomerRepository customers, SalesOrderRepository orders, SalesOrderItemRepository items, OrderOperationLogRepository logs, ProductSkuRepository skus, InventoryRepository inventory, InventoryTransactionRepository inventoryTransactions) { this.customers = customers; this.orders = orders; this.items = items; this.logs = logs; this.skus = skus; this.inventory = inventory; this.inventoryTransactions = inventoryTransactions; }
  @Transactional public SalesOrder create(CreateCommand command, String operator) {
    if (command.items().isEmpty()) throw new IllegalArgumentException("订单至少需要一个商品");
    Customer customer = customers.findByMobile(command.customerMobile()).orElseGet(() -> customers.save(Customer.create(command.customerName(), command.customerMobile())));
    List<ResolvedItem> resolved = command.items().stream().map(request -> resolve(request.skuId(), request.quantity())).toList();
    for (ResolvedItem item : resolved) if (inventory.reserve(item.sku().getId(), item.quantity()) != 1) throw new IllegalStateException("库存不足或商品不存在，无法锁定订单库存");
    BigDecimal total = resolved.stream().map(item -> item.sku().getSalePrice().multiply(BigDecimal.valueOf(item.quantity()))).reduce(BigDecimal.ZERO, BigDecimal::add);
    SalesOrder order = orders.save(SalesOrder.create("MF" + System.currentTimeMillis() + UUID.randomUUID().toString().replace("-", "").substring(0, 5).toUpperCase(), customer.getId(), total));
    resolved.forEach(item -> items.save(SalesOrderItem.create(order.getId(), item.sku().getId(), item.sku().getSkuCode(), item.sku().getSalePrice(), item.quantity())));
    logs.save(OrderOperationLog.create(order.getId(), "CREATE", null, order.getStatus(), "创建订单并锁定库存", operator)); return order;
  }
  @Transactional public SalesOrder pay(Long id, String operator) { SalesOrder order = find(id); OrderStatus from = order.getStatus(); order.markPaid(LocalDateTime.now(ZONE)); order.moveTo(OrderStatus.PENDING_SHIPMENT); for (SalesOrderItem item : items.findByOrderId(id)) { if (inventory.consumeReservation(item.getSkuId(), item.getQuantity()) != 1) throw new IllegalStateException("锁定库存不足，订单无法确认付款"); Inventory current = inventory.findById(item.getSkuId()).orElseThrow(); int after = current.getAvailableQty(); inventoryTransactions.save(InventoryTransaction.create(item.getSkuId(), "OUTBOUND", -item.getQuantity(), after + item.getQuantity(), after, "订单确认扣减：" + order.getOrderNo(), operator)); } logs.save(OrderOperationLog.create(id, "PAY", from, order.getStatus(), "确认付款并扣减库存", operator)); return order; }
  @Transactional public SalesOrder ship(Long id, String operator, String remark) { return move(id, OrderStatus.SHIPPED, "SHIP", remark == null ? "订单已发货" : remark, operator); }
  @Transactional public SalesOrder complete(Long id, String operator) { return move(id, OrderStatus.COMPLETED, "COMPLETE", "订单已完成", operator); }
  @Transactional public SalesOrder refund(Long id, String operator, String remark) { return move(id, OrderStatus.REFUNDING, "REFUND_REQUEST", remark == null ? "发起退款" : remark, operator); }
  @Transactional public SalesOrder completeRefund(Long id, String operator, String remark) { return move(id, OrderStatus.REFUNDED, "REFUND_COMPLETE", remark == null ? "退款完成，未自动回补库存" : remark, operator); }
  @Transactional public SalesOrder cancel(Long id, String operator, String remark) { SalesOrder order = find(id); if (order.getStatus() != OrderStatus.PENDING_PAYMENT) throw new IllegalStateException("仅待付款订单可直接取消"); for (SalesOrderItem item : items.findByOrderId(id)) if (inventory.release(item.getSkuId(), item.getQuantity()) != 1) throw new IllegalStateException("释放锁定库存失败"); OrderStatus from = order.getStatus(); order.moveTo(OrderStatus.CANCELLED); logs.save(OrderOperationLog.create(id, "CANCEL", from, order.getStatus(), remark == null ? "取消订单并释放库存" : remark, operator)); return order; }
  public Page<SalesOrder> search(String keyword, OrderStatus status, LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) { return orders.searchOrders(blankToNull(keyword), status == null ? null : status.name(), dateFrom, dateTo, pageable); }
  public SalesOrder findByOrderNo(String orderNo) { return orders.findByOrderNo(orderNo).orElseThrow(() -> new IllegalArgumentException("订单不存在")); }
  public List<SalesOrder> list(OrderStatus status) { return status == null ? orders.findAllByOrderByIdDesc() : orders.findByStatusOrderByIdDesc(status); }
  public SalesOrder find(Long id) { return orders.findById(id).orElseThrow(() -> new IllegalArgumentException("订单不存在")); }
  public List<SalesOrderItem> items(Long id) { return items.findByOrderId(id); }
  public List<OrderOperationLog> logs(Long id) { find(id); return logs.findByOrderIdOrderByIdDesc(id); }
  private SalesOrder move(Long id, OrderStatus target, String action, String remark, String operator) { SalesOrder order = find(id); OrderStatus from = order.getStatus(); order.moveTo(target); logs.save(OrderOperationLog.create(id, action, from, target, remark, operator)); return order; }
  private ResolvedItem resolve(Long skuId, int quantity) { if (quantity <= 0) throw new IllegalArgumentException("商品数量必须大于 0"); ProductSku sku = skus.findById(skuId).orElseThrow(() -> new IllegalArgumentException("SKU 不存在")); if (!"ACTIVE".equals(sku.getStatus())) throw new IllegalStateException("SKU 已停用"); return new ResolvedItem(sku, quantity); }
  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
  public record CreateCommand(String customerName, String customerMobile, List<CreateItem> items) {} public record CreateItem(Long skuId, int quantity) {} private record ResolvedItem(ProductSku sku, int quantity) {}
}
