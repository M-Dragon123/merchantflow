package com.merchantflow.assistant;

/**
 * 意图路由（纯规则）：根据关键词把自然语言映射到意图。
 * 未来接入 LLM Provider 时，该路由可作为工具选择（tool calling）的提示模板复用。
 */
public final class IntentRouter {
  private IntentRouter() {}

  public static Intent route(String message) {
    String text = message == null ? "" : message;
    if (containsAny(text, "补货", "备货", "库存不足", "低库存", "缺货", "预警", "进货", "安全库存"))
      return Intent.REORDER;
    if (containsAny(text, "热销", "畅销", "卖得", "排行", "top", "最好", "最受欢迎"))
      return Intent.TOP_PRODUCTS;
    if (containsAny(text, "销售额", "营收", "卖了", "销售", "业绩", "赚", "营业额", "流水"))
      return Intent.SALES;
    if (containsAny(text, "待发货", "发货", "积压", "没发"))
      return Intent.PENDING_SHIPMENT;
    if (containsAny(text, "异常", "超时", "未支付", "退款", "提醒", "问题订单"))
      return Intent.ANOMALIES;
    return Intent.HELP;
  }

  private static boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }
}
