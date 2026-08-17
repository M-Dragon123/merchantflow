package com.merchantflow.user;
import jakarta.persistence.*;
@Entity @Table(name = "sys_role") public class Role {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true) private String code;
  public String getCode() { return code; }
}
