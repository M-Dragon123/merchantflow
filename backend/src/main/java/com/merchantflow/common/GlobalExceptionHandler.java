package com.merchantflow.common;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> invalidArgument(IllegalArgumentException exception) {
    return ResponseEntity.badRequest().body(new ApiResponse<>(false, Map.of("message", exception.getMessage()), Instant.now()));
  }
  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> invalidState(IllegalStateException exception) {
    return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiResponse<>(false, Map.of("message", exception.getMessage()), Instant.now()));
  }
  /** 统一把 ResponseStatusException（如登录失败 401）转换为 ApiResponse JSON，避免空响应体导致前端误判。 */
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> responseStatus(ResponseStatusException exception) {
    String reason = exception.getReason() == null || exception.getReason().isBlank() ? "请求失败" : exception.getReason();
    return ResponseEntity.status(exception.getStatusCode()).body(new ApiResponse<>(false, Map.of("message", reason), Instant.now()));
  }
}
