package com.merchantflow.order;
import jakarta.persistence.*;
@Entity @Table(name = "customer") public class Customer {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false) private String name;
  @Column(nullable = false, unique = true) private String mobile;
  public Long getId() { return id; } public String getName() { return name; } public String getMobile() { return mobile; }
  public static Customer create(String name, String mobile) { Customer c = new Customer(); c.name = name; c.mobile = mobile; return c; }
}
