package com.merchantflow.order;

import com.merchantflow.common.ApiResponse;
import com.merchantflow.common.PageResult;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/orders") public class OrderController {
  private final OrderService service; private final CustomerRepository customers;
  public OrderController(OrderService service, CustomerRepository customers) { this.service = service; this.customers = customers; }
  @GetMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')") public ApiResponse<PageResult<OrderItem>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) OrderStatus status,
      @RequestParam(required = false) String dateFrom,
      @RequestParam(required = false) String dateTo,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size) {
    LocalDateTime from = parseDayStart(dateFrom); LocalDateTime to = parseDayAfter(dateTo);
    Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, size), 100));
    Page<SalesOrder> result = service.search(keyword, status, from, to, pageable);
    Map<Long, String> names = customers.findAllById(result.getContent().stream().map(SalesOrder::getCustomerId).toList()).stream().collect(Collectors.toMap(Customer::getId, Customer::getName));
    return ApiResponse.ok(PageResult.of(result.map(o -> item(o, names.get(o.getCustomerId())))));
  }
  @GetMapping("/by-no/{orderNo}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')") public ApiResponse<OrderDetail> byOrderNo(@PathVariable String orderNo) { SalesOrder o = service.findByOrderNo(orderNo); return ApiResponse.ok(detail(o)); }
  @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')") public ApiResponse<OrderDetail> detail(@PathVariable Long id) { return ApiResponse.ok(detail(service.find(id))); }
  @GetMapping("/{id}/logs") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR','WAREHOUSE','VIEWER')") public ApiResponse<List<LogItem>> logs(@PathVariable Long id) { return ApiResponse.ok(service.logs(id).stream().map(l -> new LogItem(l.getAction(), l.getFromStatus(), l.getToStatus(), l.getRemark(), l.getOperatorName(), l.getCreatedAt())).toList()); }
  @PostMapping @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") public ApiResponse<OrderItem> create(@Valid @RequestBody CreateOrderRequest request, Authentication auth) { SalesOrder o = service.create(new OrderService.CreateCommand(request.customerName(), request.customerMobile(), request.items().stream().map(i -> new OrderService.CreateItem(i.skuId(), i.quantity())).toList()), auth.getName()); return ApiResponse.ok(item(o)); }
  @PostMapping("/{id}/pay") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") public ApiResponse<OrderItem> pay(@PathVariable Long id, Authentication auth) { return ApiResponse.ok(item(service.pay(id, auth.getName()))); }
  @PostMapping("/{id}/ship") @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')") public ApiResponse<OrderItem> ship(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request, Authentication auth) { return ApiResponse.ok(item(service.ship(id, auth.getName(), request == null ? null : request.remark()))); }
  @PostMapping("/{id}/complete") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") public ApiResponse<OrderItem> complete(@PathVariable Long id, Authentication auth) { return ApiResponse.ok(item(service.complete(id, auth.getName()))); }
  @PostMapping("/{id}/cancel") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") public ApiResponse<OrderItem> cancel(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request, Authentication auth) { return ApiResponse.ok(item(service.cancel(id, auth.getName(), request == null ? null : request.remark()))); }
  @PostMapping("/{id}/refund") @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')") public ApiResponse<OrderItem> refund(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request, Authentication auth) { return ApiResponse.ok(item(service.refund(id, auth.getName(), request == null ? null : request.remark()))); }
  @PostMapping("/{id}/refund/complete") @PreAuthorize("hasRole('ADMIN')") public ApiResponse<OrderItem> completeRefund(@PathVariable Long id, @RequestBody(required = false) RemarkRequest request, Authentication auth) { return ApiResponse.ok(item(service.completeRefund(id, auth.getName(), request == null ? null : request.remark()))); }
  private OrderDetail detail(SalesOrder o) { return new OrderDetail(item(o), service.items(o.getId()).stream().map(i -> new LineItem(i.getSkuId(), i.getSkuNameSnapshot(), i.getQuantity(), i.getUnitPrice(), i.getSubtotalAmount())).toList()); }
  private OrderItem item(SalesOrder o) { return item(o, customers.findById(o.getCustomerId()).map(Customer::getName).orElse(null)); }
  private OrderItem item(SalesOrder o, String customerName) { return new OrderItem(o.getId(), o.getOrderNo(), o.getCustomerId(), customerName, o.getStatus(), o.getTotalAmount(), o.getCreatedAt(), o.getPaidAt()); }
  private static LocalDateTime parseDayStart(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).atStartOfDay(); }
  private static LocalDateTime parseDayAfter(String value) { return value == null || value.isBlank() ? null : LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE).plusDays(1).atStartOfDay(); }
  public record CreateOrderRequest(@NotBlank String customerName, @NotBlank String customerMobile, @NotEmpty List<@Valid CreateOrderLine> items) {} public record CreateOrderLine(@NotNull Long skuId, @Min(1) int quantity) {} public record RemarkRequest(String remark) {} public record OrderItem(Long id, String orderNo, Long customerId, String customerName, OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt, LocalDateTime paidAt) {} public record OrderDetail(OrderItem order, List<LineItem> items) {} public record LineItem(Long skuId, String skuCode, int quantity, BigDecimal unitPrice, BigDecimal subtotalAmount) {} public record LogItem(String action, String fromStatus, String toStatus, String remark, String operatorName, java.time.Instant createdAt) {}
}
