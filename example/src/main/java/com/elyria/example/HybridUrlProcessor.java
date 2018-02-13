package com.elyria.example;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import com.elyria.elyrialink.ElyriaLinkData;
import com.elyria.elyrialink.ElyriaLinkResolver.FallbackResolver;
import com.elyria.elyrialink.ElyriaLinkResolver.Interceptor;

/**
 * @author jungletian (tjsummery@gmail.com)
 */

public class HybridUrlProcessor implements Interceptor, FallbackResolver {
  private final Context context;

  public HybridUrlProcessor(Context context) {
    this.context = context;
  }

  @Nullable @Override public Intent resolve(@Nullable ElyriaLinkData entry, @NonNull String url) {
    return null;
  }

  @Nullable @Override
  public Intent intercept(@Nullable ElyriaLinkData linkData, @NonNull String url) {
    return null;
  }
}
