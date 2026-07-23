package com.example.orders;

import com.example.orders.pricing.LegacyPriceCalculator;

public class OrderService {

  private final LegacyPriceCalculator priceCalculator = new LegacyPriceCalculator();

  public String describe(Order order) {
    String coupon = order.getCouponCode();
    int price = priceCalculator.priceFor(order.getId(), coupon);
    return coupon == null
        ? "Order " + order.getId() + ": " + price
        : "Order " + order.getId() + " (coupon " + coupon + "): " + price;
  }
}
