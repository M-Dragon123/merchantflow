package com.merchantflow.product;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ProductSkuRepository extends JpaRepository<ProductSku, Long> { boolean existsBySkuCode(String skuCode); List<ProductSku> findAllByOrderByIdDesc(); }
