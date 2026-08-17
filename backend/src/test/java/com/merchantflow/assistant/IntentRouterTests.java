package com.merchantflow.assistant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class IntentRouterTests {
  @Test
  void routesReorderKeywords() {
    assertEquals(Intent.REORDER, IntentRouter.route("哪些商品建议补货？"));
    assertEquals(Intent.REORDER, IntentRouter.route("低库存预警有哪些"));
    assertEquals(Intent.REORDER, IntentRouter.route("安全库存不足的要不要进货"));
  }

  @Test
  void routesOtherIntents() {
    assertEquals(Intent.TOP_PRODUCTS, IntentRouter.route("最近卖得最好的商品？"));
    assertEquals(Intent.SALES, IntentRouter.route("今天销售额怎么样"));
    assertEquals(Intent.PENDING_SHIPMENT, IntentRouter.route("有多少待发货订单"));
    assertEquals(Intent.ANOMALIES, IntentRouter.route("有什么异常订单要处理"));
  }

  @Test
  void fallsBackToHelp() {
    assertEquals(Intent.HELP, IntentRouter.route("你好"));
    assertEquals(Intent.HELP, IntentRouter.route(""));
    assertEquals(Intent.HELP, IntentRouter.route(null));
  }
}
