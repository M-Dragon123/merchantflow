package com.merchantflow.common;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 全局异常处理：所有 MVC 层异常都在这里转成统一的 ApiResponse JSON，
 * 避免异常被转发到 /error 后被安全链拦截成裸 401（前端会误判为登录过期）。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> invalidArgument(IllegalArgumentException exception) {
    return error(HttpStatus.BAD_REQUEST, exception.getMessage());
  }

  @ExceptionHandler(IllegalStateException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> invalidState(IllegalStateException exception) {
    return error(HttpStatus.CONFLICT, exception.getMessage());
  }

  /** 请求体不是合法 JSON / 字段类型不匹配（如 null 传入 int 字段）。 */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> unreadableBody(HttpMessageNotReadableException exception) {
    return error(HttpStatus.BAD_REQUEST, "请求体不是合法的 JSON 或字段类型不匹配");
  }

  /** @Valid 参数校验失败，返回首个字段错误信息。 */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> invalidBody(MethodArgumentNotValidException exception) {
    String message = exception.getBindingResult().getFieldErrors().stream()
        .findFirst().map(err -> err.getField() + " " + err.getDefaultMessage()).orElse("参数校验失败");
    return error(HttpStatus.BAD_REQUEST, message);
  }

  /** 请求方法不支持（如对只读接口发 POST）。 */
  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> methodNotSupported(HttpRequestMethodNotSupportedException exception) {
    return error(HttpStatus.METHOD_NOT_ALLOWED, "不支持的请求方法 " + exception.getMethod());
  }

  /** 未知接口路径（需 spring.mvc.throw-exception-if-no-handler-found=true）。 */
  @ExceptionHandler(NoHandlerFoundException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> noHandler(NoHandlerFoundException exception) {
    return error(HttpStatus.NOT_FOUND, "接口不存在");
  }

  /** 未知静态资源路径（Spring 6.1+ 由 ResourceHttpRequestHandler 抛出）。 */
  @ExceptionHandler(NoResourceFoundException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> noResource(NoResourceFoundException exception) {
    return error(HttpStatus.NOT_FOUND, "资源不存在");
  }

  /**
   * @PreAuthorize 的 AccessDeniedException 必须继续向上抛，由 Spring Security
   * 的 ExceptionTranslationFilter 转成 403 JSON；不能在 MVC 层吞掉。
   */
  @ExceptionHandler(AccessDeniedException.class)
  void rethrowAccessDenied(AccessDeniedException exception) throws AccessDeniedException {
    throw exception;
  }

  /** 兜底：未预期的异常统一返回 500 JSON，避免裸错误页与堆栈泄露。 */
  @ExceptionHandler(Exception.class)
  ResponseEntity<ApiResponse<Map<String, String>>> unexpected(Exception exception) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误，请稍后重试");
  }

  /** 统一把 ResponseStatusException（如登录失败 401）转换为 ApiResponse JSON，避免空响应体导致前端误判。 */
  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<ApiResponse<Map<String, String>>> responseStatus(ResponseStatusException exception) {
    String reason = exception.getReason() == null || exception.getReason().isBlank() ? "请求失败" : exception.getReason();
    return error(exception.getStatusCode(), reason);
  }

  private static ResponseEntity<ApiResponse<Map<String, String>>> error(HttpStatusCode status, String message) {
    return ResponseEntity.status(status).body(new ApiResponse<>(false, Map.of("message", message), Instant.now()));
  }
}
