package com.example.orders;

import org.jspecify.annotations.Nullable;

public class Order {

  private final String id;
  private final @Nullable String couponCode;

  public Order(String id, @Nullable String couponCode) {
    this.id = id;
    this.couponCode = couponCode;
  }

  public String getId() {
    return id;
  }

  public @Nullable String getCouponCode() {
    return couponCode;
  }
}
