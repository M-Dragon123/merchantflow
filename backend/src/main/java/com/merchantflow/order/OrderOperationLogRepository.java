package com.merchantflow.order;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderOperationLogRepository extends JpaRepository<OrderOperationLog, Long> {
  List<OrderOperationLog> findByOrderIdOrderByIdDesc(Long orderId);
}
