package com.merchantflow.product;
import jakarta.persistence.*;
@Entity @Table(name = "product_spu") public class ProductSpu {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "category_id", nullable = false) private Long categoryId;
  @Column(nullable = false) private String name;
  @Column(nullable = false) private String status = "ACTIVE";
  public Long getId() { return id; } public Long getCategoryId() { return categoryId; } public String getName() { return name; } public String getStatus() { return status; }
  public static ProductSpu create(Long categoryId, String name) { ProductSpu s = new ProductSpu(); s.categoryId = categoryId; s.name = name; return s; }
  public void rename(String newName) { name = newName; }
}
