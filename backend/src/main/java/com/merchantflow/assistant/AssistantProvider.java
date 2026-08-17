package com.merchantflow.assistant;

import java.util.List;

/**
 * AI 助手应答 Provider（可插拔）：
 * <ul>
 *   <li>默认实现 {@link RuleBasedAssistantProvider}：内置规则引擎，基于工具结果生成中文回复，零外部依赖；</li>
 *   <li>接入真实大模型时：新增实现类（如 LlmAssistantProvider），把 userMessage 与工具结果拼接为提示词调用 LLM API，
 *       再通过配置 merchantflow.assistant.provider=llm 切换即可，其余代码不变。</li>
 * </ul>
 * 注意：助手只允许执行只读工具（见 AssistantService 的 collect* 只读查询），不得直接写数据库；
 * 任何库存调整必须由用户二次确认后走既有的库存调整接口（记录操作人与原因）。
 */
public interface AssistantProvider {
  /** 根据意图与工具结果生成面向用户的自然语言回复。 */
  String answer(String userMessage, Intent intent, List<AssistantDtos.ToolResult> results);
}
