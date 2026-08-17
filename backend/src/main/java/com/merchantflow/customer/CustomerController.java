package com.merchantflow.customer;

import com.merchantflow.common.ApiResponse;
import com.merchantflow.common.PageResult;
import com.merchantflow.order.Customer;
import com.merchantflow.order.CustomerRepository;
import com.merchantflow.order.OrderStatus;
import com.merchantflow.order.SalesOrder;
import com.merchantflow.order.SalesOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** 客户管理（管理员 / 运营）。 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {
  private final CustomerRepository customers;
  private final SalesOrderRepository orders;

  public CustomerController(CustomerRepository customers, SalesOrderRepository orders) {
    this.customers = customers;
    this.orders = orders;
  }

  @GetMapping
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public ApiResponse<PageResult<CustomerItem>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int size) {
    Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.min(Math.max(1, size), 100));
    Page<Customer> result = customers.search(blankToNull(keyword), pageable);
    Map<Long, Long> counts = new HashMap<>();
    List<Long> ids = result.getContent().stream().map(Customer::getId).toList();
    if (!ids.isEmpty()) {
      for (Object[] row : orders.countByCustomerIds(ids)) counts.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
    }
    return ApiResponse.ok(PageResult.of(result.map(c -> new CustomerItem(c.getId(), c.getName(), c.getMobile(), counts.getOrDefault(c.getId(), 0L)))));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public ApiResponse<CustomerDetail> detail(@PathVariable Long id) {
    Customer c = customers.findById(id).orElseThrow(() -> new IllegalArgumentException("客户不存在"));
    List<OrderLine> orderLines = orders.findByCustomerIdOrderByIdDesc(id).stream().limit(20)
        .map(o -> new OrderLine(o.getId(), o.getOrderNo(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt())).toList();
    return ApiResponse.ok(new CustomerDetail(c.getId(), c.getName(), c.getMobile(), orderLines));
  }

  private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

  public record CustomerItem(Long id, String name, String mobile, long orderCount) {}
  public record OrderLine(Long id, String orderNo, OrderStatus status, BigDecimal totalAmount, LocalDateTime createdAt) {}
  public record CustomerDetail(Long id, String name, String mobile, List<OrderLine> recentOrders) {}
}
