package com.example.orders.pricing;

public class LegacyPriceCalculator {

  public int priceFor(String orderId, String couponCode) {
    return couponCode == null ? 1000 : 800;
  }
}
