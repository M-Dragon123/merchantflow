package com.merchantflow.assistant;

import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 规则引擎 Provider（默认）：基于意图与工具结果组装中文回复。
 * 不调用任何外部大模型；接入真实 LLM 时新增 Provider 实现并通过
 * merchantflow.assistant.provider 切换。
 */
@Component
@ConditionalOnProperty(name = "merchantflow.assistant.provider", havingValue = "rule-based", matchIfMissing = true)
public class RuleBasedAssistantProvider implements AssistantProvider {

  @Override
  public String answer(String userMessage, Intent intent, List<AssistantDtos.ToolResult> results) {
    String tools = results.isEmpty() ? "（暂无数据）" : String.join("\n", results.stream().map(AssistantDtos.ToolResult::summary).toList());
    return switch (intent) {
      case REORDER -> "根据当前库存，建议关注以下商品：\n" + tools
          + "\n\n如需补货，可点击下方建议卡片「一键补货」，确认后执行入库（会写入库存流水）。";
      case TOP_PRODUCTS -> "近 30 天热销排行如下：\n" + tools;
      case SALES -> "经营概况如下：\n" + tools;
      case PENDING_SHIPMENT -> "待发货情况如下：\n" + tools;
      case ANOMALIES -> "异常订单提醒如下：\n" + tools;
      case HELP -> "我可以帮你查询经营数据（只读，不会直接修改数据）：\n"
          + "· 补货建议：\"哪些商品建议补货？\"\n"
          + "· 热销排行：\"最近卖得最好的商品？\"\n"
          + "· 经营概况：\"今天销售额怎么样？\"\n"
          + "· 待发货：\"有多少待发货订单？\"\n"
          + "· 异常订单：\"有什么异常订单？\"\n"
          + "需要执行库存调整时，我会给出建议并由你二次确认。";
    };
  }
}
