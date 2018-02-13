package com.elyria.elyrialink.internal;

import android.os.Bundle;
import android.support.annotation.Nullable;
import com.elyria.elyrialink.ElyriaLinker;
import com.elyria.elyrialink.IllegalElyriaLinkException;
import java.io.IOException;
import java.lang.reflect.Type;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

public class Utils {

  private Utils() {
    throw new IllegalStateException("No instance.");
  }

  public static <T> T fromJson(String jsonString, Type type, String from)
      throws IllegalElyriaLinkException {
    try {
      Object object = ElyriaLinker.getJsonConverter().fromJson(jsonString, type);
      return valueCast(object, from);
    } catch (IllegalElyriaLinkException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalElyriaLinkException("Parse json string failed", e);
    }
  }

  public @Nullable static String toJson(Object object, Type type) {
    try {
      return ElyriaLinker.getJsonConverter().toJson(object, type);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public static void checkHasKey(Bundle args, String key) throws IllegalElyriaLinkException {
    if (args == null || !args.containsKey(key)) {
      throw new IllegalElyriaLinkException(key + " is missing in Bundle.");
    }
  }

  private static String getStringValue(Bundle bundle, String key) {
    return bundle.getString(key);
  }

  public static boolean parseBoolean(Bundle bundle, String key) {
    return Boolean.parseBoolean(getStringValue(bundle, key));
  }

  public static int parseInt(Bundle bundle, String key, int defaultValue) {
    String value = getStringValue(bundle, key);
    try {
      return Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static long parseLong(Bundle bundle, String key, long defaultValue) {
    String value = getStringValue(bundle, key);
    try {
      return Long.parseLong(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static float parseFloat(Bundle bundle, String key, float defaultValue) {
    String value = getStringValue(bundle, key);
    try {
      return Float.parseFloat(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  public static double parseDouble(Bundle bundle, String key, double defaultValue) {
    String value = getStringValue(bundle, key);
    try {
      return Double.parseDouble(value);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  private static <T> T valueCast(Object object, String who) throws IllegalElyriaLinkException {
    try {
      if (object == null) {
        throw new NullPointerException("JsonConverter for: " + who + "returns null");
      }
      return (T) object;
    } catch (ClassCastException e) {
      throw new IllegalElyriaLinkException("Wrong class cast.", e);
    }
  }

  public static <T> T checkNotNull(T obj) {
    if (obj == null) {
      throw new NullPointerException();
    }
    return obj;
  }

  public static <T> T checkNotNull(T reference, Object errorMessage) {
    if (reference == null) {
      throw new NullPointerException(String.valueOf(errorMessage));
    }
    return reference;
  }

  static void checkArgument(boolean condition) {
    if (!condition) {
      throw new IllegalArgumentException();
    }
  }

  @SuppressWarnings("unused")
  public static void checkState(boolean expression, Object errorMessage) {
    if (!expression) {
      throw new IllegalStateException(String.valueOf(errorMessage));
    }
  }
}
