package com.merchantflow.user;

import com.merchantflow.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

/** 员工与角色管理（仅管理员）。 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserAccountRepository users;
  private final RoleRepository roles;
  private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

  public UserController(UserAccountRepository users, RoleRepository roles) {
    this.users = users;
    this.roles = roles;
  }

  @GetMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<List<UserItem>> list() {
    return ApiResponse.ok(users.findAll().stream().map(this::item).toList());
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserItem> create(@Valid @RequestBody CreateUserRequest request) {
    if (users.findByUsername(request.username()).isPresent()) throw new IllegalArgumentException("用户名已存在");
    Role role = roles.findByCode(request.roleCode()).orElseThrow(() -> new IllegalArgumentException("角色不存在：" + request.roleCode()));
    UserAccount user = users.save(UserAccount.create(request.username(), encoder.encode(request.password()), request.name()));
    user.replaceRoles(Set.of(role));
    users.save(user);
    return ApiResponse.ok(item(user));
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserItem> updateStatus(@PathVariable Long id, @RequestBody StatusRequest request, Authentication auth) {
    UserAccount user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    if (!request.status() && user.getUsername().equals(auth.getName()))
      throw new IllegalArgumentException("不能停用自己的账号");
    user.updateStatus(request.status());
    users.save(user);
    return ApiResponse.ok(item(user));
  }

  @PutMapping("/{id}/roles")
  @PreAuthorize("hasRole('ADMIN')")
  public ApiResponse<UserItem> updateRoles(@PathVariable Long id, @Valid @RequestBody RolesRequest request) {
    UserAccount user = users.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    Set<Role> roleSet = new HashSet<>();
    for (String code : request.roleCodes()) {
      roleSet.add(roles.findByCode(code).orElseThrow(() -> new IllegalArgumentException("角色不存在：" + code)));
    }
    user.replaceRoles(roleSet);
    users.save(user);
    return ApiResponse.ok(item(user));
  }

  private UserItem item(UserAccount user) {
    return new UserItem(user.getId(), user.getUsername(), user.getName(), user.isStatus(),
        user.getRoles().stream().map(Role::getCode).sorted().toList(), user.getCreatedAt());
  }

  public record CreateUserRequest(@NotBlank String username, @NotBlank String password, @NotBlank String name, @NotBlank String roleCode) {}
  public record StatusRequest(@NotNull Boolean status) {}
  public record RolesRequest(@NotEmpty List<@NotBlank String> roleCodes) {}
  public record UserItem(Long id, String username, String name, boolean status, List<String> roles, LocalDateTime createdAt) {}
}
