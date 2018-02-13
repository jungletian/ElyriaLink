package com.elyria.elyrialink;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import com.elyria.elyrialink.internal.QueryBinder;
import com.elyria.elyrialink.internal.Utils;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author jungletian (tjsummery@gmail.com)
 */
public class ElyriaLinker {

  public static final String EXTRA_ELYRIA_LINK = "com.elyria.elyrialink.EXTRA_ELYRIA_LINK";
  public static final String EXTRA_ELYRIA_LINK_REFERRER =
      "com.elyria.elyrialink.EXTRA_ELYRIA_LINK_REFERRER";

  public static final String EXTRA_ELYRIA_LINK_REDIRECT =
      "com.elyria.elyrialink.EXTRA_ELYRIA_LINK_REDIRECT";

  private static final String TAG = "ElyriaLinker";
  private static JsonConverter JSON_CONVERTER;

  static final Map<Class<?>, Constructor<? extends QueryBinder>> BINDINGS = new LinkedHashMap<>();
  private static boolean DEBUG = false;

  private ElyriaLinker() {

  }

  public static void setDebug(boolean enable) {
    ElyriaLinker.DEBUG = enable;
  }

  public static boolean isSamePage(@NonNull String l, @NonNull String r) {
    return TextUtils.equals(trimQuery(l), trimQuery(r));
  }

  public static void putElyriaLink(@NonNull Intent intent, @Nullable String link) {
    putString(intent, EXTRA_ELYRIA_LINK, link);
  }

  public static @Nullable String getElyriaLink(Intent intent) {
    return getStringFromIntent(intent, EXTRA_ELYRIA_LINK);
  }

  public static @Nullable String getSourceLink(Intent intent) {
    return getStringFromIntent(intent, EXTRA_ELYRIA_LINK_REFERRER);
  }

  public static void putSourceLink(@NonNull Intent intent, @Nullable String link) {
    putString(intent, EXTRA_ELYRIA_LINK_REFERRER, link);
  }

  public static @Nullable String getRedirectLink(Intent intent) {
    return getStringFromIntent(intent, EXTRA_ELYRIA_LINK_REDIRECT);
  }

  public static void putRedirectLink(@NonNull Intent intent, @Nullable String link) {
    putString(intent, EXTRA_ELYRIA_LINK_REDIRECT, link);
  }

  private static void putString(Intent intent, String key, String value) {
    Utils.checkNotNull(intent, "intent == null");
    Utils.checkNotNull(key, "key == null");
    intent.putExtra(key, value);
  }

  private static @Nullable String getStringFromIntent(Intent intent, String key) {
    if (intent != null && intent.hasExtra(key)) {
      return intent.getStringExtra(key);
    } else {
      return null;
    }
  }

  @SuppressWarnings("unused") public static String trimQuery(String elyriaUrl) {
    if (!TextUtils.isEmpty(elyriaUrl)) {
      String[] result = TextUtils.split(elyriaUrl.trim(), Pattern.quote("?"));
      String url = result[0];
      if (!TextUtils.isEmpty(url)) {
        return url.trim().replaceAll("/$", "");
      }
    }
    return elyriaUrl;
  }

  static @NonNull Bundle buildExtraFromUrl(@NonNull String url) {
    Utils.checkNotNull(url, "url == null");
    Bundle bundle = new Bundle();
    putQueryParameters(bundle, Uri.parse(url));
    return bundle;
  }

  private static void putQueryParameters(Bundle parameters, Uri linkUri) {
    Set<String> queryParameterNames = linkUri.getQueryParameterNames();
    if (queryParameterNames != null && !queryParameterNames.isEmpty()) {
      for (String name : queryParameterNames) {
        parameters.putString(name, linkUri.getQueryParameter(name));
      }
    }
  }

  public static void bind(@NonNull Activity activity) throws IllegalElyriaLinkException {
    doBind(activity);
  }

  public static void bind(@NonNull Fragment fragment) throws IllegalElyriaLinkException {
    doBind(fragment);
  }

  public static void bind(@NonNull android.support.v4.app.Fragment fragment)
      throws IllegalElyriaLinkException {
    doBind(fragment);
  }

  private static void doBind(Object object) throws IllegalElyriaLinkException {
    Class<?> cls = object.getClass();
    Constructor<? extends QueryBinder> constructor = findBindingConstructorForClass(cls);
    if (constructor == null) {
      throw new IllegalElyriaLinkException(
          "Unable to parse binding constructor for " + cls.getName());
    }
    try {
      QueryBinder queryBinder = constructor.newInstance();
      queryBinder.bind(object);
    } catch (InstantiationException e) {
      throw new RuntimeException("Unable to invoke " + constructor, e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException("Unable to invoke " + constructor, e);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof RuntimeException) {
        throw (RuntimeException) cause;
      }
      if (cause instanceof Error) {
        throw (Error) cause;
      }
      throw new RuntimeException("Unable to create binding instance.", cause);
    }
  }

  private static Constructor<? extends QueryBinder> findBindingConstructorForClass(Class<?> cls) {
    Constructor<? extends QueryBinder> bindingConstructor = BINDINGS.get(cls);
    if (bindingConstructor != null) {
      if (DEBUG) Log.d(TAG, "HIT: Cached in binding map.");
      return bindingConstructor;
    }
    String clsName = cls.getName();
    try {
      Class<?> bindingClass = Class.forName(clsName + "QueryBinder");
      bindingConstructor = (Constructor<? extends QueryBinder>) bindingClass.getConstructor();
      if (DEBUG) Log.d(TAG, "HIT: Loaded binding class and constructor.");
    } catch (ClassNotFoundException e) {
      throw new RuntimeException("Unable to parse binding constructor for " + clsName, e);
    } catch (NoSuchMethodException e) {
      throw new RuntimeException("Unable to parse binding constructor for " + clsName, e);
    }
    BINDINGS.put(cls, bindingConstructor);
    return bindingConstructor;
  }

  public static void installJsonConverter(@NonNull JsonConverter converter) {
    if (converter == null) throw new NullPointerException("converter == null");
    ElyriaLinker.JSON_CONVERTER = converter;
  }

  public static JsonConverter getJsonConverter() {
    if (JSON_CONVERTER == null) {
      throw new IllegalStateException("Must call installJsonConverter() before get");
    }
    return JSON_CONVERTER;
  }

  public interface JsonConverter {
    Object fromJson(@NonNull String jsonString, Type type) throws IOException;

    String toJson(@NonNull Object value, Type type) throws IOException;
  }
}
