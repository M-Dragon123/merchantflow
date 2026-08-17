package com.merchantflow.user;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
@Entity @Table(name = "sys_user") public class UserAccount {
  @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
  @Column(nullable = false, unique = true) private String username;
  @Column(name = "password_hash", nullable = false) private String passwordHash;
  @Column(nullable = false) private String name;
  /** 数据库列为 TINYINT(1)，显式声明 JDBC 类型避免 Hibernate validate 把 boolean 期望成 BIT。 */
  @JdbcTypeCode(SqlTypes.TINYINT) @Column(nullable = false) private boolean status;
  @Column(name = "created_at", insertable = false, updatable = false) private LocalDateTime createdAt;
  @ManyToMany(fetch = FetchType.EAGER) @JoinTable(name = "sys_user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id")) private Set<Role> roles = new HashSet<>();
  public Long getId() { return id; } public String getUsername() { return username; } public String getPasswordHash() { return passwordHash; } public String getName() { return name; } public boolean isStatus() { return status; } public LocalDateTime getCreatedAt() { return createdAt; } public Set<Role> getRoles() { return roles; }
  public static UserAccount create(String username, String passwordHash, String name) { UserAccount u = new UserAccount(); u.username = username; u.passwordHash = passwordHash; u.name = name; u.status = true; return u; }
  public void updateStatus(boolean value) { status = value; }
  public void replaceRoles(Set<Role> newRoles) { roles.clear(); roles.addAll(newRoles); }
}
