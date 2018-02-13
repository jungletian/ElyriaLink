package com.elyria.example.link;

import com.google.gson.annotations.SerializedName;

/**
 * Created by twocity on 9/21/16.
 */

public class Product {

  @SerializedName("id") private final long productId;
  @SerializedName("name") private final String productName;

  public Product(long productId, String productName) {
    this.productId = productId;
    this.productName = productName;
  }

  @Override public String toString() {
    return productId + ',' + productName;
  }
}
