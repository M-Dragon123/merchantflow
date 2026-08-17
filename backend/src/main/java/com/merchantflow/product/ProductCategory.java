package com.merchantflow.product;
import jakarta.persistence.*;
@Entity @Table(name = "product_category") public class ProductCategory {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(name = "parent_id") private Long parentId;
  @Column(nullable = false) private String name;
  @Column(nullable = false) private Integer sort = 0;
  public Long getId() { return id; } public Long getParentId() { return parentId; } public String getName() { return name; } public Integer getSort() { return sort; }
  public static ProductCategory create(String name, Long parentId, Integer sort) { ProductCategory c = new ProductCategory(); c.name = name; c.parentId = parentId; c.sort = sort == null ? 0 : sort; return c; }
}
