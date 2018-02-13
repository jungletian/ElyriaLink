package com.elyria.example;

import android.text.TextUtils;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

public class Utils {
  private Utils() {

  }

  static String valueOrDefault(String value, String defaultValue) {
    if (TextUtils.isEmpty(value)) {
      return defaultValue;
    }
    return value;
  }
}
