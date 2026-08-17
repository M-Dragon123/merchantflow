package com.merchantflow.auth;
import com.merchantflow.common.ApiResponse;
import com.merchantflow.security.JwtService;
import com.merchantflow.user.UserAccount;
import com.merchantflow.user.UserAccountRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController @RequestMapping("/api/v1/auth") public class AuthController {
  private final UserAccountRepository users; private final JwtService jwt; private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
  public AuthController(UserAccountRepository users, JwtService jwt) { this.users = users; this.jwt = jwt; }
  @PostMapping("/login") public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    UserAccount user = users.findByUsername(request.username()).filter(UserAccount::isStatus).orElse(null);
    if (user == null || !encoder.matches(request.password(), user.getPasswordHash())) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户名或密码错误");
    List<String> roles = user.getRoles().stream().map(r -> r.getCode()).sorted().toList();
    return ApiResponse.ok(new LoginResponse(jwt.createToken(user.getId(), user.getUsername(), roles), new CurrentUser(user.getId(), user.getUsername(), user.getName(), roles)));
  }
  @GetMapping("/me") public ApiResponse<CurrentUser> me(Authentication authentication) { UserAccount u = users.findByUsername(authentication.getName()).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效，请重新登录")); return ApiResponse.ok(new CurrentUser(u.getId(), u.getUsername(), u.getName(), u.getRoles().stream().map(r -> r.getCode()).sorted().toList())); }
  public record LoginRequest(@NotBlank String username, @NotBlank String password) {} public record LoginResponse(String accessToken, CurrentUser user) {} public record CurrentUser(Long id, String username, String name, List<String> roles) {}
}
