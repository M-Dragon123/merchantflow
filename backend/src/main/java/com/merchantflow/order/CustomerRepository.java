package com.merchantflow.order;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
public interface CustomerRepository extends JpaRepository<Customer, Long> {
  Optional<Customer> findByMobile(String mobile);
  @Query(value = "select * from customer where (:keyword is null or name like concat('%', :keyword, '%') or mobile like concat('%', :keyword, '%')) order by id desc",
      countQuery = "select count(*) from customer where (:keyword is null or name like concat('%', :keyword, '%') or mobile like concat('%', :keyword, '%'))",
      nativeQuery = true)
  Page<Customer> search(@Param("keyword") String keyword, Pageable pageable);
}
