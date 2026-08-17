package com.merchantflow.order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, Long> {
  List<SalesOrder> findAllByOrderByIdDesc();
  List<SalesOrder> findByStatusOrderByIdDesc(OrderStatus status);
  Optional<SalesOrder> findByOrderNo(String orderNo);

  /** 订单检索：订单号/客户姓名/手机号关键字 + 状态 + 创建时间范围（含分页）。 */
  @Query(value = """
      select o.* from sales_order o
      join customer c on c.id = o.customer_id
      where (:keyword is null or o.order_no like concat('%', :keyword, '%')
         or c.name like concat('%', :keyword, '%') or c.mobile like concat('%', :keyword, '%'))
        and (:status is null or o.status = :status)
        and (:dateFrom is null or o.created_at >= :dateFrom)
        and (:dateTo is null or o.created_at < :dateTo)
      order by o.id desc
      """,
      countQuery = """
      select count(*) from sales_order o
      join customer c on c.id = o.customer_id
      where (:keyword is null or o.order_no like concat('%', :keyword, '%')
         or c.name like concat('%', :keyword, '%') or c.mobile like concat('%', :keyword, '%'))
        and (:status is null or o.status = :status)
        and (:dateFrom is null or o.created_at >= :dateFrom)
        and (:dateTo is null or o.created_at < :dateTo)
      """,
      nativeQuery = true)
  Page<SalesOrder> searchOrders(@Param("keyword") String keyword, @Param("status") String status,
      @Param("dateFrom") LocalDateTime dateFrom, @Param("dateTo") LocalDateTime dateTo, Pageable pageable);

  // ---- 工作台聚合（销售额口径：已付款且未取消/未退款完成） ----
  @Query(value = "select count(*) from sales_order where created_at >= :from", nativeQuery = true)
  long countCreatedSince(@Param("from") LocalDateTime from);

  @Query(value = "select coalesce(sum(total_amount), 0) from sales_order where paid_at >= :from and status not in ('CANCELLED','REFUNDED')", nativeQuery = true)
  BigDecimal sumPaidSince(@Param("from") LocalDateTime from);

  @Query(value = "select count(*) from sales_order where status = 'PENDING_SHIPMENT'", nativeQuery = true)
  long countPendingShipment();

  @Query(value = "select count(*) from sales_order where status = 'REFUNDING'", nativeQuery = true)
  long countRefunding();

  @Query(value = "select date(paid_at) as day, coalesce(sum(total_amount), 0) as amount from sales_order where paid_at >= :from and status not in ('CANCELLED','REFUNDED') group by date(paid_at) order by day asc", nativeQuery = true)
  List<Object[]> salesTrendSince(@Param("from") LocalDateTime from);

  @Query(value = "select i.sku_id, s.sku_code, sum(i.quantity) as qty, coalesce(sum(i.subtotal_amount), 0) as amount from sales_order_item i join sales_order o on o.id = i.order_id join product_sku s on s.id = i.sku_id where o.paid_at >= :from and o.status not in ('CANCELLED','REFUNDED') group by i.sku_id, s.sku_code order by qty desc, amount desc limit 10", nativeQuery = true)
  List<Object[]> topProductsSince(@Param("from") LocalDateTime from);

  @Query(value = "select o.id, o.order_no, o.total_amount, o.created_at from sales_order o where o.status = 'PENDING_PAYMENT' and o.created_at < :cutoff order by o.created_at asc limit 20", nativeQuery = true)
  List<Object[]> overduePaymentOrders(@Param("cutoff") LocalDateTime cutoff);

  /** 客户维度：最近订单。 */
  List<SalesOrder> findByCustomerIdOrderByIdDesc(Long customerId);

  @Query(value = "select o.customer_id as cid, count(*) as cnt from sales_order o where o.customer_id in (:ids) group by o.customer_id", nativeQuery = true)
  List<Object[]> countByCustomerIds(@Param("ids") List<Long> ids);
}
