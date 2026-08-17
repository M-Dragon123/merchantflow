package com.merchantflow.assistant;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** 助手模块 DTO。 */
public final class AssistantDtos {
  private AssistantDtos() {}

  /** 只读工具执行结果：name 为工具名，summary 为渲染后的文本。 */
  public record ToolResult(String name, String summary) {}

  /** 可执行的建议（仅补货类），由前端二次确认后调用现有库存调整接口；delta 为建议入库数量。 */
  public record Suggestion(String type, Long skuId, String skuCode, String title, String description, int delta) {}

  public record ChatRequest(@NotBlank String message) {}

  public record ChatResponse(String reply, List<Suggestion> suggestions) {}
}
