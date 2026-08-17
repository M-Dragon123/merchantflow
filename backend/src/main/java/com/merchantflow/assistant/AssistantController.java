package com.merchantflow.assistant;

import com.merchantflow.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** AI 运营助手（管理员 / 运营）：只读查询 + 建议，不直接写库。 */
@RestController
@RequestMapping("/api/v1/assistant")
public class AssistantController {
  private final AssistantService service;

  public AssistantController(AssistantService service) {
    this.service = service;
  }

  @PostMapping("/chat")
  @PreAuthorize("hasAnyRole('ADMIN','OPERATOR')")
  public ApiResponse<AssistantDtos.ChatResponse> chat(@Valid @RequestBody AssistantDtos.ChatRequest request) {
    return ApiResponse.ok(service.chat(request.message()));
  }
}
